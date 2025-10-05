package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;

import java.util.List;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Transactional
@SpringBootTest
@TestPropertySource(properties = {
    "cloudflare.r2.access-key-id=dummy",
    "cloudflare.r2.secret-access-key=dummy",
    "cloudflare.r2.endpoint=http://localhost",
    "cloudflare.r2.bucket-name=test-bucket",
    "PINECONE_API_KEY=dummy",
    "PINECONE_INDEX_HOST=http://localhost",
    "PINECONE_NAMESPACE=test"
})
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MemeCreateServiceTest {

    @Autowired
    private MemeCreateService memeCreateService;

    @Autowired
    private MemeRepository memeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private MemeCategoryRepository memeCategoryRepository;

    @MockBean
    private ImageUploadService imageUploadService;

    @MockBean
    private MemeVectorIndexService vectorIndexService;

    @AfterEach
    void tearDown() {
        memeCategoryRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("createMeme: 업로드/저장/카테고리연결/인덱싱 호출")
    void createMeme_end_to_end_basic_flow() {
        // given
        Category cat1 = categoryRepository.save(Category.builder().name("예능").imgUrl("cat1.jpg").build());
        Category cat2 = categoryRepository.save(Category.builder().name("연예").imgUrl("cat2.jpg").build());

        MemeCreateRequest req = new MemeCreateRequest(
            "무야호", "무한도전", "기쁨 표현", "2018",
            "[#무한도전,#무야호]", List.of(cat1.getId(), cat2.getId())
        );

        MultipartFile fakeFile = Mockito.mock(MultipartFile.class);
        when(fakeFile.isEmpty()).thenReturn(false);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://cdn/img.png");

        // when
        long id = memeCreateService.createMeme(req, fakeFile);

        // then
        Meme saved = memeRepository.findById(id).orElseThrow();
        then(saved.getTitle()).isEqualTo("무야호");
        then(saved.getImgUrl()).isEqualTo("https://cdn/img.png");
        then(saved.getFlag()).isEqualTo(Meme.Flag.NORMAL);

        List<MemeCategory> links = memeCategoryRepository.findAll();
        then(links).hasSize(2);
        then(links).extracting(mc -> mc.getCategory().getId())
            .containsExactlyInAnyOrder(cat1.getId(), cat2.getId());

        // indexing should be attempted once
        verify(vectorIndexService, times(1)).index(any(Meme.class));
    }

    @Test
    @DisplayName("indexing 예외가 발생해도 저장은 성공한다")
    void indexingFailureDoesNotBreakCreate() {
        // given
        MemeCreateRequest req = new MemeCreateRequest(
            "테스트", "출처", "맥락", "2025", "[#태그]", List.of()
        );
        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("url");
        Mockito.doThrow(new RuntimeException("pinecone down")).when(vectorIndexService).index(any(Meme.class));

        // when
        long id = memeCreateService.createMeme(req, file);

        // then
        then(memeRepository.findById(id)).isPresent();
        verify(vectorIndexService, times(1)).index(any(Meme.class));
    }
}
