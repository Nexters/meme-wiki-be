package spring.memewikibe.external;

import spring.memewikibe.domain.meme.Meme;

public record MemeDoc(
    String id,
    String doc
) {

    public static MemeDoc from(Meme meme) {
        StringBuilder docBuilder = new StringBuilder();

        if (meme.getTitle() != null && !meme.getTitle().trim().isEmpty()) {
            docBuilder.append("제목: ").append(meme.getTitle());
        }

        if (meme.getOrigin() != null && !meme.getOrigin().trim().isEmpty()) {
            if (!docBuilder.isEmpty()) docBuilder.append(" | ");
            docBuilder.append("기원: ").append(meme.getOrigin());
        }

        if (meme.getUsageContext() != null && !meme.getUsageContext().trim().isEmpty()) {
            if (!docBuilder.isEmpty()) docBuilder.append(" | ");
            docBuilder.append("사용 맥락: ").append(meme.getUsageContext());
        }

        if (meme.getTrendPeriod() != null && !meme.getTrendPeriod().trim().isEmpty()) {
            if (!docBuilder.isEmpty()) docBuilder.append(" | ");
            docBuilder.append("유행 시기: ").append(meme.getTrendPeriod());
        }

        if (meme.getHashtags() != null && !meme.getHashtags().trim().isEmpty()) {
            if (!docBuilder.isEmpty()) docBuilder.append(" | ");
            docBuilder.append("태그: ").append(meme.getHashtags());
        }

        return new MemeDoc(
            meme.getId().toString(),
            docBuilder.toString()
        );
    }
}
