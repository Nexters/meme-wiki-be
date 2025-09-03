package spring.memewikibe.external.request;

import spring.memewikibe.external.MemeDoc;

import java.util.List;

public record ClovaRerankerRequest(
    List<MemeDoc> documents,
    String query
) {
}
