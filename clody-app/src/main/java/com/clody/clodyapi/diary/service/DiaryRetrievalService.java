package com.clody.clodyapi.diary.service;

import com.clody.clodyapi.diary.controller.dto.response.DiaryCreatedTimeResponse;
import com.clody.clodyapi.diary.controller.dto.response.DiaryResponse;
import com.clody.clodyapi.diary.mapper.DiaryMapper;
import com.clody.clodyapi.diary.usecase.DiaryQueryUsecase;
import com.clody.domain.diary.dto.DiaryContent;
import com.clody.domain.diary.dto.DiaryDateInfo;
import com.clody.domain.diary.dto.response.DiaryCreatedInfo;
import com.clody.domain.diary.service.DiaryQueryService;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.clody.domain.reply.Reply;
import com.clody.domain.reply.dto.DequeuedMessage;
import com.clody.domain.reply.repository.ReplyRepository;
import com.clody.domain.reply.service.RodyProcessor;
import com.clody.support.dto.type.ErrorType;
import com.clody.support.exception.BusinessException;
import com.clody.support.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryRetrievalService implements DiaryQueryUsecase {

  private final DiaryQueryService diaryQueryService;
  private final ReplyRepository replyRepository;
  private final RodyProcessor rodyProcessor;


  @Override
  public DiaryCreatedTimeResponse getCreatedTime(int year, int month, int date) {
    DiaryDateInfo diaryDateInfo = DiaryMapper.toDiaryDateInfo(year, month, date);
    DiaryCreatedInfo diaryCreatedInfo = diaryQueryService.getCreatedDateTime(diaryDateInfo);

    return DiaryMapper.toDiaryCreatedTimeResponse(diaryCreatedInfo);
  }


  @Override
  @Transactional
  public DiaryCreatedTimeResponse getCreatedTime(int year, int month, int date, Boolean adWatched) {
    if (adWatched == null || !adWatched) {
      return getCreatedTime(year, month, date);
    }

    // 광고 시청 시 즉시 답변 처리
    DiaryDateInfo diaryDateInfo = DiaryMapper.toDiaryDateInfo(year, month, date);
    DiaryCreatedInfo diaryCreatedInfo = diaryQueryService.getCreatedDateTime(diaryDateInfo);

    // 광고 시청 시 즉시 답변 생성 및 처리
    processImmediateReply(LocalDate.of(year, month, date));

    return DiaryMapper.toDiaryCreatedTimeResponse(diaryCreatedInfo.withIsFromAdTrue());
  }

  /**
   * 광고 시청 후 즉시 답변을 처리하는 메서드
   */
  @Transactional
  public void processImmediateReply(LocalDate diaryDate) {
    try {
      // 해당 날짜의 Reply 조회
      Reply reply = replyRepository.findByUserIdAndDiaryCreatedDate(
              JwtUtil.getLoginMemberId(), diaryDate);

      // 이미 답변이 있는 경우 처리하지 않음
      if (reply.getContent() != null && reply.getReplyInfo().checkReadable()) {
        return;
      }
      // 즉시 답변 생성 및 처리
      DequeuedMessage message = createImmediateReplyMessage(reply);
      rodyProcessor.createReply(message);

      // 답변 상태를 SUCCEED, isFromAd를 true로 설정
      reply.updateStatusToSUCCEED();
      reply.updateIsFromAdToTrue();

      // 답변 상태 저장
      replyRepository.save(reply);

    } catch (Exception e) {
      throw new BusinessException(ErrorType.INTERNAL_SERVER_ERROR);
    }
  }


  /**
   * 즉시 처리할 답변 메시지 생성
   */
  private DequeuedMessage createImmediateReplyMessage(Reply reply) {
    // 다이어리 내용 가져오기
    LocalDate diaryDate = reply.getDiaryCreatedDate();
    DiaryDateInfo dateInfo = DiaryMapper.toDiaryDateInfo(
            diaryDate.getYear(), diaryDate.getMonthValue(), diaryDate.getDayOfMonth());
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


  @Override
  public DiaryResponse getDiary(final int year, final int month, final int date) {
    DiaryDateInfo diaryDateInfo = DiaryMapper.toDiaryDateInfo(year, month, date);
    List<DiaryContent> diaryContentList = diaryQueryService.getDiary(diaryDateInfo);
    boolean userHasDeletedDiary = diaryQueryService.isUserHasDeletedDiary(diaryDateInfo);
    return DiaryMapper.toDiaryResponse(diaryContentList, userHasDeletedDiary);
  }
}