package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.memewikibe.annotation.UnitTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleKoreanEmbeddingService 단위 테스트")
class SimpleKoreanEmbeddingServiceTest {

    @Mock
    private EmbeddingService mockEmbeddingService;

    private SimpleKoreanEmbeddingService sut;

    @BeforeEach
    void setUp() {
        sut = new SimpleKoreanEmbeddingService(mockEmbeddingService);
    }

    @Test
    @DisplayName("embed: 정상적인 한글 텍스트를 임베딩 벡터로 변환")
    void embed_succeeds_withValidKoreanText() {
        // given
        String koreanText = "안녕하세요 밈 위키입니다";
        float[] expectedEmbedding = new float[]{0.1f, 0.2f, 0.3f};
        when(mockEmbeddingService.embed(koreanText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(koreanText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(koreanText);
    }

    @Test
    @DisplayName("embed: null 텍스트를 delegate에 전달")
    void embed_delegatesNullText_toUnderlyingService() {
        // given
        String nullText = null;
        float[] expectedEmbedding = new float[]{0.0f, 0.0f, 0.0f};
        when(mockEmbeddingService.embed(nullText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(nullText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(nullText);
    }

    @Test
    @DisplayName("embed: 빈 문자열을 delegate에 전달")
    void embed_delegatesEmptyString_toUnderlyingService() {
        // given
        String emptyText = "";
        float[] expectedEmbedding = new float[]{0.0f, 0.0f, 0.0f};
        when(mockEmbeddingService.embed(emptyText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(emptyText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(emptyText);
    }

    @Test
    @DisplayName("embed: 공백만 있는 문자열을 delegate에 전달")
    void embed_delegatesBlankString_toUnderlyingService() {
        // given
        String blankText = "   ";
        float[] expectedEmbedding = new float[]{0.0f, 0.0f, 0.0f};
        when(mockEmbeddingService.embed(blankText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(blankText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(blankText);
    }

    @Test
    @DisplayName("embed: 긴 한글 텍스트를 임베딩 벡터로 변환")
    void embed_succeeds_withLongKoreanText() {
        // given
        String longText = "밈(Meme)은 문화적 정보나 아이디어가 사람들 사이에서 전파되는 것을 설명하는 개념입니다. " +
            "인터넷 밈은 일반적으로 이미지, 동영상, 해시태그 등의 형태로 소셜 미디어를 통해 빠르게 확산됩니다.";
        float[] expectedEmbedding = new float[1536]; // typical embedding dimension
        when(mockEmbeddingService.embed(longText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(longText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        assertThat(result.length).isEqualTo(1536);
        verify(mockEmbeddingService).embed(longText);
    }

    @Test
    @DisplayName("embed: 혼합된 한글/영어 텍스트를 처리")
    void embed_succeeds_withMixedKoreanEnglishText() {
        // given
        String mixedText = "Meme은 밈입니다 #trending #인기밈";
        float[] expectedEmbedding = new float[]{0.5f, 0.6f, 0.7f};
        when(mockEmbeddingService.embed(mixedText)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(mixedText);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(mixedText);
    }

    @Test
    @DisplayName("embed: 특수문자가 포함된 텍스트를 처리")
    void embed_succeeds_withSpecialCharacters() {
        // given
        String textWithSpecialChars = "안녕하세요! 😀 #밈 @사용자 https://example.com";
        float[] expectedEmbedding = new float[]{0.8f, 0.9f, 1.0f};
        when(mockEmbeddingService.embed(textWithSpecialChars)).thenReturn(expectedEmbedding);

        // when
        float[] result = sut.embed(textWithSpecialChars);

        // then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(mockEmbeddingService).embed(textWithSpecialChars);
    }
}
