package spring.memewikibe.external.google;

import org.assertj.core.api.BDDAssertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import spring.memewikibe.external.google.application.ImageGenerator;
import spring.memewikibe.external.google.client.response.GenerateContentResponse;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ImageGeneratorTest {
    private final ImageGenerator imageGenerator;

    public ImageGeneratorTest(ImageGenerator imageGenerator) {
        this.imageGenerator = imageGenerator;
    }

    @Disabled
    @DisplayName("응답을 알아보자")
    @Test
    void test() {
        // given
        String prompt = "A kawaii-style sticker of a happy red panda wearing a tiny bamboo hat. It'\"'\"'s munching on a green bamboo leaf. The design features bold, clean outlines, simple cel-shading, and a vibrant color palette. The background must be white.";
        // when
        GenerateContentResponse response = imageGenerator.generateImage(prompt);
        // then
        BDDAssertions.then(response).isNotNull();
    }

    @Disabled
    @Test
    void 이미지수정요청() {
        // given
        // when
        GenerateContentResponse response = imageGenerator.generateImageWithExistingImage("얼굴을 트럼프와 푸틴으로 바꿔줘", "https://img.meme-wiki.net/meme-wiki/da1b97e5-f3ef-431c-b4d1-7e6c6ebec14d.jpg");
        // then
        System.out.println(response);
        BDDAssertions.then(response).isNotNull();
    }
}
