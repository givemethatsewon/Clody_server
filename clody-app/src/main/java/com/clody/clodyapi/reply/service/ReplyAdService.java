package com.clody.clodyapi.reply.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.clody.clodyapi.reply.controller.dto.ReplyAdRequest;
import com.clody.clodyapi.reply.usecase.ReplyAdUsecase;
import com.clody.domain.diary.dto.DiaryContent;
import com.clody.domain.diary.dto.DiaryDateInfo;
import com.clody.domain.diary.service.DiaryQueryService;
import com.clody.domain.reply.Reply;
import com.clody.domain.reply.ReplyProcessStatus;
import com.clody.domain.reply.dto.DequeuedMessage;
import com.clody.domain.reply.service.RodyProcessor;
import com.clody.infra.models.reply.repository.ReplyRepositoryAdapter;
import com.clody.support.dto.type.ErrorType;
import com.clody.support.exception.BusinessException;
import com.clody.support.security.util.JwtUtil;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyAdService implements ReplyAdUsecase {
    private final ReplyRepositoryAdapter replyRepositoryAdapter;
    private final DiaryQueryService diaryQueryService;
    private final RodyProcessor rodyProcessor;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 광고 시청 후 즉시 답변을 처리하는 메서드
     */
    @Override
    @Transactional
    public void processAdReply(ReplyAdRequest replyAdRequest) {
        LocalDate diaryDate = LocalDate.of(replyAdRequest.year(), replyAdRequest.month(), replyAdRequest.date());
        Long userId = JwtUtil.getLoginMemberId();

        // 해당 날짜의 Reply 조회
        log.info(diaryDate.toString());
        log.info(userId.toString());
        Reply reply = replyRepositoryAdapter.findByUserIdAndDiaryCreatedDate(userId, diaryDate);

        // 이미 답변이 있는 경우 처리하지 않음
        if (reply.getContent() != null && reply.getReplyInfo().checkReadable()) {
            return;
        }

        // 다이어리 내용을 기반으로 메시지 생성
        DequeuedMessage message = createImmediateReplyMessage(reply);
        log.info(message.toString());

        // RodyProcessorImpl을 직접 사용하여 content 업데이트
        rodyProcessor.createReply(message);

        // 명시적 엔티티 리프레시
        entityManager.refresh(reply);

        // 여기서 content가 업데이트 된 reply를 다시 조회
        reply = replyRepositoryAdapter.findById(reply.getId());

        // 답변 상태를 SUCCEED로 설정
        reply.updateReplyProcessStatus(ReplyProcessStatus.SUCCEED);
        reply.offPushNotification();

        // 저장 - 명시적으로 저장 -> dirdty check 활용하도록 변경
        replyRepositoryAdapter.save(reply);

    }


    /**
     * 즉시 처리할 답변 메시지 생성
     */
    private DequeuedMessage createImmediateReplyMessage(Reply reply) {
        // 다이어리 내용 가져오기
        LocalDate diaryDate = reply.getDiaryCreatedDate();
        DiaryDateInfo dateInfo = DiaryDateInfo.of(diaryDate.getYear(), diaryDate.getMonthValue(), diaryDate.getDayOfMonth());
        List<DiaryContent> diaryContents = diaryQueryService.getDiary(dateInfo);

        // 다이어리 내용을 문자열로 변환
        String content = IntStream.range(0, diaryContents.size())
                .mapToObj(i -> (i + 1) + ". " + diaryContents.get(i).content())
                .collect(Collectors.joining(", "));

        return DequeuedMessage.of(
                reply.getId(),
                reply.getUser().getId(),
                content,
                -1,
                reply.getReplyType()
        );
    }

    /**
     * 광고 시청 종료 후 isFromAd 값을 true로 업데이트하는 메서드
     */
    @Override
    @Transactional
    public void updateAdReply(ReplyAdRequest replyAdRequest) {
        LocalDate diaryDate = LocalDate.of(replyAdRequest.year(), replyAdRequest.month(), replyAdRequest.date());
        Long userId = JwtUtil.getLoginMemberId();

        // 1. 우선 content 생성 여부를 트랜잭션 없이 확인
        if (waitForContent(userId, diaryDate)) {
            // 2. 성공 시에만 짧은 트랜잭션으로 상태 업데이트
            Reply reply = replyRepositoryAdapter.findByUserIdAndDiaryCreatedDate(userId, diaryDate);
            reply.updateIsFromAd(true);
            replyRepositoryAdapter.save(reply);
        } else {
            throw new BusinessException(ErrorType.REPLY_CONTENT_TIMEOUT);
        }
    }

    // 트랜잭션 없이 콘텐츠 생성 대기
    private boolean waitForContent(Long userId, LocalDate diaryDate) {
        int maxAttempts = 10;
        int initialWaitMs = 500;
        int maxWaitMs = 2000;
        int currentWaitMs = initialWaitMs;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                // 각 조회를 별도의 트랜잭션으로 수행
                boolean hasContent = checkReplyContent(userId, diaryDate);
                if (hasContent) {
                    return true;
                }

                // 지수 백오프 방식으로 대기 시간 증가 (최대 2초까지)
                Thread.sleep(Math.min(currentWaitMs, maxWaitMs));
                currentWaitMs *= 1.5;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }


    // 읽기 전용 트랜잭션으로 콘텐츠 확인만 수행
    public boolean checkReplyContent(Long userId, LocalDate diaryDate) {
        try {
            Reply reply = replyRepositoryAdapter.findByUserIdAndDiaryCreatedDate(userId, diaryDate);
            entityManager.refresh(reply);
            return reply != null && reply.getContent() != null;
        } catch (Exception e) {
            log.warn("Failed to check reply content for user {} on date {}: {}",
                    userId, diaryDate, e.getMessage());
            return false;
        }
    }
}
