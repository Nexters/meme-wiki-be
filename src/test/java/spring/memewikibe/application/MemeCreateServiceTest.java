package spring.memewikibe.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.domain.meme.Category;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @MockitoBean
    private ImageUploadService imageUploadService;

    @MockitoBean
    private MemeVectorIndexService vectorIndexService;

    @AfterEach
    void tearDown() {
        memeCategoryRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        memeRepository.deleteAllInBatch();
        reset(imageUploadService, vectorIndexService);
    }

    @Test
    @DisplayName("createMeme: 업로드/저장/카테고리연결 성공")
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

        // indexing should be attempted once after transaction commit
        verify(vectorIndexService, times(1)).index(any(Meme.class));
    }

    @Test
    @DisplayName("인덱싱 예외가 발생해도 밈 생성은 성공한다")
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

    @Test
    @DisplayName("빈 파일 업로드 시 예외가 발생한다")
    void createMeme_throwsException_whenFileIsEmpty() {
        // given
        Category category = categoryRepository.save(Category.builder().name("예능").imgUrl("cat.jpg").build());
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", List.of(category.getId())
        );

        MultipartFile emptyFile = Mockito.mock(MultipartFile.class);
        when(emptyFile.isEmpty()).thenReturn(true);
        when(imageUploadService.uploadImage(any(MultipartFile.class)))
            .thenThrow(new IllegalArgumentException("파일이 비어있습니다."));

        // when & then
        assertThatThrownBy(() -> memeCreateService.createMeme(req, emptyFile))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("파일이 비어있습니다.");

        // 저장이 롤백되어야 함
        then(memeRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("이미지 업로드 실패 시 예외가 발생하고 밈이 저장되지 않는다")
    void createMeme_throwsException_whenImageUploadFails() {
        // given
        Category category = categoryRepository.save(Category.builder().name("예능").imgUrl("cat.jpg").build());
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", List.of(category.getId())
        );

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(imageUploadService.uploadImage(any(MultipartFile.class)))
            .thenThrow(new RuntimeException("R2 업로드 실패"));

        // when & then
        assertThatThrownBy(() -> memeCreateService.createMeme(req, file))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("R2 업로드 실패");

        // 저장이 롤백되어야 함
        then(memeRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 ID로 생성 시 카테고리 연결이 되지 않는다")
    void createMeme_skipsNonExistentCategories() {
        // given
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", List.of(999L, 1000L)
        );

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://cdn/img.png");

        // when
        long id = memeCreateService.createMeme(req, file);

        // then
        Meme saved = memeRepository.findById(id).orElseThrow();
        then(saved.getTitle()).isEqualTo("밈제목");

        // 존재하지 않는 카테고리이므로 연결이 없어야 함
        List<MemeCategory> links = memeCategoryRepository.findAll();
        then(links).isEmpty();
    }

    @Test
    @DisplayName("카테고리 ID가 null이어도 밈 생성은 성공한다")
    void createMeme_succeeds_whenCategoryIdsIsNull() {
        // given
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", null
        );

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://cdn/img.png");

        // when
        long id = memeCreateService.createMeme(req, file);

        // then
        Meme saved = memeRepository.findById(id).orElseThrow();
        then(saved.getTitle()).isEqualTo("밈제목");

        List<MemeCategory> links = memeCategoryRepository.findAll();
        then(links).isEmpty();
    }

    @Test
    @DisplayName("카테고리 ID 리스트가 비어있어도 밈 생성은 성공한다")
    void createMeme_succeeds_whenCategoryIdsIsEmpty() {
        // given
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", Collections.emptyList()
        );

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://cdn/img.png");

        // when
        long id = memeCreateService.createMeme(req, file);

        // then
        Meme saved = memeRepository.findById(id).orElseThrow();
        then(saved.getTitle()).isEqualTo("밈제목");

        List<MemeCategory> links = memeCategoryRepository.findAll();
        then(links).isEmpty();
    }

    @Test
    @DisplayName("일부 카테고리만 존재하는 경우 존재하는 카테고리만 연결된다")
    void createMeme_linksOnlyExistingCategories() {
        // given
        Category existingCategory = categoryRepository.save(Category.builder().name("예능").imgUrl("cat.jpg").build());
        MemeCreateRequest req = new MemeCreateRequest(
            "밈제목", "출처", "맥락", "2025", "[#태그]", List.of(existingCategory.getId(), 999L)
        );

        MultipartFile file = Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(imageUploadService.uploadImage(any(MultipartFile.class))).thenReturn("https://cdn/img.png");

        // when
        long id = memeCreateService.createMeme(req, file);

        // then
        Meme saved = memeRepository.findById(id).orElseThrow();
        then(saved.getTitle()).isEqualTo("밈제목");

        // 존재하는 카테고리만 연결되어야 함
        List<MemeCategory> links = memeCategoryRepository.findAll();
        then(links).hasSize(1);
        then(links.getFirst().getCategory().getId()).isEqualTo(existingCategory.getId());
    }
}
