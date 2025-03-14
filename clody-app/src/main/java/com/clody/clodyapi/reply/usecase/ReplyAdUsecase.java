package com.clody.clodyapi.reply.usecase;

import com.clody.clodyapi.reply.controller.dto.ReplyAdRequest;

public interface ReplyAdUsecase {
    /**
     * 광고 시청 후 즉시 답변을 생성합니다.
     *
     * @param replyAdRequest 답장 날짜 정보
     */
    void processAdReply(ReplyAdRequest replyAdRequest);

    /**
     * 광고 시청 종료 후 isFromAd 값을 true로 업데이트합니다.
     *
     * @param replyAdRequest 답장 날짜 정보
     */
    void updateAdReply(ReplyAdRequest replyAdRequest);
}
