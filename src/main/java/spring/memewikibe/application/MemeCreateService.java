package spring.memewikibe.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.common.util.HashtagParser;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;

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
        return savedMeme.getId();
    }

    public long createMemeUsingCrawler(MemeCreateRequest request, MultipartFile imageFile) {
        String imageUrl = imageUploadService.uploadImage(imageFile);

        Meme meme = Meme.crawlerMeme(
            request.title(),
            request.origin(),
            request.usageContext(),
            request.trendPeriod(),
            imageUrl,
            HashtagParser.toJson(request.hashtags())
        );

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

        log.info("크롤러를 통한 밈 생성 완료: {}", savedMeme.getId());
        return savedMeme.getId();
    }
} 