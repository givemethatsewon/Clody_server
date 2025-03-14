package com.clody.clodyapi.reply.controller;

import com.clody.clodyapi.reply.controller.dto.ReplyAdRequest;
import com.clody.domain.reply.dto.ReplyResponse;
import com.clody.support.constants.HeaderConstants;
import com.clody.support.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "답변", description = "답변과 관련된 기능을 수행하는 API 입니다.")
@RequestMapping("/api/v1")
@RestController
public interface ReplyApi {

  @GetMapping("/reply")
  @Operation(summary = "답변 조회 ", description = "액세스 토큰과 년/월/일을 통해 답변을 조회합니다.")
  ResponseEntity<ApiResponse<ReplyResponse>> getReply(
      @RequestHeader(HeaderConstants.AUTHORIZATION) final String accessToken,
      @RequestParam @Parameter(name = "연도", description = "조회할 연도", required = true) final int year,
      @RequestParam @Parameter(name = "달", description = "조회할 달", required = true) final int month,
      @RequestParam @Parameter(name = "일", description = "조회할 일", required = true) final int date);

  @Operation(summary = "광고 시청 시작하여 즉시 답장 생성", description = "광고 시청을 시작한 경우 즉시 답장을 생성합니다.")
  @PostMapping("/reply/ad/start")
  ResponseEntity<ApiResponse<Void>> startAdViewing(
          @RequestBody @Parameter(name = "광고 시청 정보", description = "광고 시청 대상 날짜 정보", required = true)
          ReplyAdRequest request
  );

  @Operation(summary = "광고 시청 종료하여 isFromAd값 true로 업데이트", description = "광고 시청을 정상적으로 완료한 경우 isFromAd값을 true로 업데이트합니다.")
  @PatchMapping("/reply/ad/end")
    ResponseEntity<ApiResponse<Void>> endAdViewing(
            @RequestBody @Parameter(name = "광고 시청 정보", description = "광고 시청 대상 날짜 정보", required = true)
            ReplyAdRequest request
    );
}




