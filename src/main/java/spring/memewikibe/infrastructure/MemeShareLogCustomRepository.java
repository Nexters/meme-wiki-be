package spring.memewikibe.infrastructure;

import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.time.Duration;
import java.util.List;

public interface MemeShareLogCustomRepository {
    List<MemeSimpleResponse> findTopMemesByShareCountWithin(Duration duration, int limit);
}