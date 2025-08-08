package spring.memewikibe.api.controller.meme.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record MemeCreateRequest(
    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 100, message = "제목은 100자를 초과할 수 없습니다.")
    String title,

    @NotBlank(message = "출처는 필수입니다.")
    @Size(max = 1000, message = "출처는 1000자를 초과할 수 없습니다.")
    String origin,

    @NotBlank(message = "사용 맥락은 필수입니다.")
    @Size(max = 1000, message = "사용 맥락은 1000자를 초과할 수 없습니다.")
    String usageContext,

    @NotBlank(message = "트렌드 기간은 필수입니다.")
    @Size(max = 100, message = "트렌드 기간은 100자를 초과할 수 없습니다.")
    String trendPeriod,

    @Size(max = 200, message = "해시태그는 200자를 초과할 수 없습니다.")
    String hashtags,

    List<Long> categoryIds
) {}