package test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import spring.memewikibe.MemewikiBeApplication;
import spring.memewikibe.application.rag.MemeEmbeddingService;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;

/**
 * Simple test to verify Pinecone vector store operations work after the fix
 */
@SpringBootApplication
public class PineconeFixTest {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "local");
        
        ApplicationContext context = SpringApplication.run(MemewikiBeApplication.class, args);
        
        try {
            MemeRepository memeRepository = context.getBean(MemeRepository.class);
            MemeEmbeddingService embeddingService = context.getBean(MemeEmbeddingService.class);
            
            // Create a test meme
            Meme testMeme = new Meme();
            testMeme.setTitle("Test Meme");
            testMeme.setUsageContext("This is a test context for vector store");
            testMeme.setOrigin("Test Origin");
            testMeme.setHashtags("#test #vector #store");
            testMeme.setImgUrl("https://example.com/test.jpg");
            
            // Save the meme first
            Meme savedMeme = memeRepository.save(testMeme);
            System.out.println("Created test meme with ID: " + savedMeme.getId());
            
            // Try to embed and store the meme in vector store
            embeddingService.embedAndStoreMeme(savedMeme);
            System.out.println("SUCCESS: Meme embedded and stored without error!");
            
            // Clean up
            memeRepository.delete(savedMeme);
            System.out.println("Test completed successfully - no Pinecone panic error occurred");
            
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.exit(0);
    }
}