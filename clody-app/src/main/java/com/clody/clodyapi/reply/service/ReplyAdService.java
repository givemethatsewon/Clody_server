package com.clody.clodyapi.reply.service;

import com.clody.clodyapi.diary.mapper.DiaryMapper;
import com.clody.clodyapi.reply.controller.dto.ReplyAdRequest;
import com.clody.clodyapi.reply.usecase.ReplyAdUsecase;
import com.clody.domain.diary.dto.DiaryContent;
import com.clody.domain.diary.dto.DiaryDateInfo;
import com.clody.domain.diary.dto.response.DiaryCreatedInfo;
import com.clody.domain.diary.service.DiaryQueryService;
import com.clody.domain.reply.Reply;
import com.clody.domain.reply.ReplyProcessStatus;
import com.clody.domain.reply.dto.DequeuedMessage;
import com.clody.domain.reply.service.RodyProcessor;
import com.clody.infra.models.reply.repository.ReplyRepositoryAdapter;
import com.clody.support.security.util.JwtUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyAdService implements ReplyAdUsecase {
    private final ReplyRepositoryAdapter replyRepositoryAdapter;
    private final DiaryQueryService diaryQueryService;
    private final RodyProcessor rodyProcessor;
    private final EntityManager entityManager;

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
//            reply.updateIsFromAd(true);

        // 저장 - 명시적으로 저장 -> dirdty check 활용하도록 변경
//        replyRepositoryAdapter.save(reply);

        // 업데이트 후에 조회하여 로깅
//        Reply updatedReply = replyRepositoryAdapter.findById(reply.getId());
//        log.info("Reply updated: {}, content: {}", updatedReply.getId(), updatedReply.getContent());
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
                reply.getVersion(),
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

        // 해당 날짜의 Reply 조회
        Reply reply = replyRepositoryAdapter.findByUserIdAndDiaryCreatedDate(userId, diaryDate);

        // 이미 답변이 있는 경우 처리하지 않음
        if (reply.getContent() != null && reply.getReplyInfo().checkReadable()) {
            return;
        }
        reply.updateIsFromAd(true); // 답변 상태를 SUCCEED로 설정
        reply.updateVersion(-1);    // 광고 버전으로 설정(알림 가지 않도록 처리)

    }

}
