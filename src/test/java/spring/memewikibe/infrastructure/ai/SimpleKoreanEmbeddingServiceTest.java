package spring.memewikibe.infrastructure.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimpleKoreanEmbeddingServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @InjectMocks
    private SimpleKoreanEmbeddingService simpleKoreanEmbeddingService;

    private float[] expectedEmbedding;

    @BeforeEach
    void setUp() {
        expectedEmbedding = new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f};
    }

    @Test
    @DisplayName("Should delegate to EmbeddingService for normal text")
    void shouldDelegateToEmbeddingServiceForNormalText() {
        // Given
        String text = "안녕하세요";
        when(embeddingService.embed(text)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(text);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(text);
    }

    @Test
    @DisplayName("Should handle null text by delegating to EmbeddingService")
    void shouldHandleNullTextByDelegating() {
        // Given
        when(embeddingService.embed(null)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(null);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(null);
    }

    @Test
    @DisplayName("Should handle blank text by delegating to EmbeddingService")
    void shouldHandleBlankTextByDelegating() {
        // Given
        String blankText = "   ";
        when(embeddingService.embed(blankText)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(blankText);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(blankText);
    }

    @Test
    @DisplayName("Should handle empty text by delegating to EmbeddingService")
    void shouldHandleEmptyTextByDelegating() {
        // Given
        String emptyText = "";
        when(embeddingService.embed(emptyText)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(emptyText);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(emptyText);
    }

    @Test
    @DisplayName("Should handle Korean text with special characters")
    void shouldHandleKoreanTextWithSpecialCharacters() {
        // Given
        String text = "밈 #해시태그 @멘션 https://example.com";
        when(embeddingService.embed(text)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(text);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(text);
    }

    @Test
    @DisplayName("Should handle very long Korean text")
    void shouldHandleVeryLongKoreanText() {
        // Given
        String longText = "밈 ".repeat(1000);
        when(embeddingService.embed(anyString())).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(longText);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(longText);
    }

    @Test
    @DisplayName("Should return embedding vector with correct dimensions")
    void shouldReturnEmbeddingVectorWithCorrectDimensions() {
        // Given
        String text = "테스트 밈";
        float[] embedding768 = new float[768];
        for (int i = 0; i < 768; i++) {
            embedding768[i] = (float) Math.random();
        }
        when(embeddingService.embed(text)).thenReturn(embedding768);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(text);

        // Then
        assertThat(result).hasSize(768);
        assertThat(result).isEqualTo(embedding768);
    }

    @Test
    @DisplayName("Should handle mixed Korean and English text")
    void shouldHandleMixedKoreanAndEnglishText() {
        // Given
        String mixedText = "밈 meme 문화 culture";
        when(embeddingService.embed(mixedText)).thenReturn(expectedEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(mixedText);

        // Then
        assertThat(result).isEqualTo(expectedEmbedding);
        verify(embeddingService).embed(mixedText);
    }

    @Test
    @DisplayName("Should delegate when embedding service returns empty array")
    void shouldDelegateWhenEmbeddingServiceReturnsEmptyArray() {
        // Given
        String text = "테스트";
        float[] emptyEmbedding = new float[0];
        when(embeddingService.embed(text)).thenReturn(emptyEmbedding);

        // When
        float[] result = simpleKoreanEmbeddingService.embed(text);

        // Then
        assertThat(result).isEmpty();
        verify(embeddingService).embed(text);
    }
}
