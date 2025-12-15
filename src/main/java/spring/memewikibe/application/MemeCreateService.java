package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemeCreateService {

    private final MemeRepository memeRepository;
    private final CategoryRepository categoryRepository;
    private final MemeCategoryRepository memeCategoryRepository;
    private final ImageUploadService imageUploadService;
    private final MemeVectorIndexService vectorIndexService;

    public long createMeme(MemeCreateRequest request, MultipartFile imageFile) {
        String imageUrl = imageUploadService.uploadImage(imageFile);

        Meme meme = Meme.builder()
            .title(request.title())
            .origin(request.origin())
            .usageContext(request.usageContext())
            .trendPeriod(request.trendPeriod())
            .imgUrl(imageUrl)
            .hashtags(HashtagParser.toJson(request.hashtags()))
            .flag(Meme.Flag.NORMAL)
            .build();

        Meme savedMeme = memeRepository.save(meme);

        Optional.ofNullable(request.categoryIds())
            .filter(ids -> !ids.isEmpty())
            .map(categoryRepository::findAllById)
            .map(categories -> categories.stream()
                .map(category -> MemeCategory.builder()
                    .meme(savedMeme)
                    .category(category)
                    .build())
                .toList())
            .ifPresent(memeCategoryRepository::saveAll);

        log.info("밈 생성 완료: {}", savedMeme.getId());
        registerPostCommitIndexing(savedMeme);
        return savedMeme.getId();
    }

    private void registerPostCommitIndexing(Meme meme) {
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        vectorIndexService.index(meme);
                    } catch (Exception e) {
                        log.warn("[afterCommit] Failed to index meme {} to Pinecone: {}", meme.getId(), e.getMessage());
                    }
                }
            }
        );
    }
} 