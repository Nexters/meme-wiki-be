package spring.memewikibe.application;

import spring.memewikibe.api.controller.meme.response.MemeSimpleResponse;

import java.util.List;

public interface MemeAggregationLookUpService {
    List<MemeSimpleResponse> getMostFrequentSharedMemes();

    List<MemeSimpleResponse> getMostFrequentCustomMemes();

    List<MemeSimpleResponse> getMostPopularMemes();
}
