package spring.memewikibe.api.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.application.ImageUploadService;
import spring.memewikibe.application.MemeLookUpService;
import spring.memewikibe.application.MemeCreateService;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;
import spring.memewikibe.api.controller.meme.request.MemeUpdateRequest;
import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemeRepository memeRepository;
    private final ImageUploadService imageUploadService;
    private final MemeLookUpService memeLookUpService;
    private final MemeCreateService memeCreateService;
    private final CategoryRepository categoryRepository;
    private final MemeCategoryRepository memeCategoryRepository;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    /**
     * 관리자 로그인 페이지
     */
    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (isAuthenticated(session)) {
            return "redirect:/admin/memes";
        }
        return "admin/login";
    }

    /**
     * 관리자 로그인 처리
     */
    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        log.info("Admin login attempt: username={}", username);
        
        if (adminUsername.equals(username) && adminPassword.equals(password)) {
            session.setAttribute("admin_authenticated", true);
            log.info("Admin login successful");
            return "redirect:/admin/memes";
        } else {
            log.warn("Admin login failed: invalid credentials");
            redirectAttributes.addFlashAttribute("error", "잘못된 사용자명 또는 비밀번호입니다.");
            return "redirect:/admin/login";
        }
    }

    /**
     * 관리자 로그아웃
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }

    /**
     * 밈 관리 메인 페이지 (리스트 + 추가 폼)
     */
    @GetMapping("/memes")
    public String memesPage(Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        List<Object[]> memeWithCategories = memeRepository.findAllWithCategoryNamesOrderByIdDesc();
        List<Meme> memes = memeRepository.findAllByOrderByIdDesc();
        List<CategoryResponse> categories = memeLookUpService.getAllCategories();
        
        // 밈별 카테고리 정보 매핑
        Map<Long, List<String>> memeCategoryMap = memeWithCategories.stream()
            .filter(row -> row[1] != null) // 카테고리가 있는 경우만
            .collect(Collectors.groupingBy(
                row -> ((Meme) row[0]).getId(),
                Collectors.mapping(row -> (String) row[1], Collectors.toList())
            ));
        
        model.addAttribute("memes", memes);
        model.addAttribute("memeCategoryMap", memeCategoryMap);
        model.addAttribute("categories", categories);
        model.addAttribute("totalCount", memes.size());
        
        log.info("Admin accessing memes page. Total memes: {}, Total categories: {}", 
                 memes.size(), categories.size());
        return "admin/memes";
    }

    /**
     * 새 밈 추가 처리
     */
    @PostMapping("/memes")
    public String addMeme(@RequestParam String title,
                         @RequestParam String origin,
                         @RequestParam String usageContext,
                         @RequestParam String hashtags,
                         @RequestParam(required = false) String imgUrl,
                         @RequestParam(required = false) MultipartFile imageFile,
                         @RequestParam(required = false) String trendPeriod,
                         @RequestParam(required = false) List<Long> categoryIds,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            // 이미지 URL 결정 (파일 업로드 우선, 없으면 URL 사용)
            String finalImgUrl = null;
            
            if (imageFile != null && !imageFile.isEmpty()) {
                log.info("📁 파일 업로드 시작: {}", imageFile.getOriginalFilename());
                finalImgUrl = imageUploadService.uploadImage(imageFile);
                log.info("✅ 파일 업로드 완료: {}", finalImgUrl);
            } else if (imgUrl != null && !imgUrl.trim().isEmpty()) {
                finalImgUrl = imgUrl.trim();
                log.info("🔗 이미지 URL 사용: {}", finalImgUrl);
            }

            // MemeCreateRequest 생성 (trendPeriod는 필수이므로 기본값 설정)
            String validTrendPeriod = (trendPeriod != null && !trendPeriod.trim().isEmpty()) 
                ? trendPeriod.trim() : "2024";
                
            MemeCreateRequest createRequest = new MemeCreateRequest(
                title.trim(),
                origin.trim(),
                usageContext.trim(),
                validTrendPeriod,
                hashtags.trim(),
                categoryIds != null ? categoryIds : List.of()
            );

            // 밈 생성 (카테고리 포함)
            Long memeId;
            if (imageFile != null && !imageFile.isEmpty()) {
                memeId = memeCreateService.createMeme(createRequest, imageFile);
            } else {
                // 이미지 파일이 없는 경우 기존 방식 사용
                Meme meme = Meme.builder()
                        .title(title.trim())
                        .origin(origin.trim())
                        .usageContext(usageContext.trim())
                        .hashtags(hashtags.trim())
                        .imgUrl(finalImgUrl)
                        .trendPeriod(validTrendPeriod)
                        .build();

                Meme savedMeme = memeRepository.save(meme);
                memeId = savedMeme.getId();
                
                // 카테고리 연결
                if (categoryIds != null && !categoryIds.isEmpty()) {
                    categoryRepository.findAllById(categoryIds)
                        .forEach(category -> {
                            MemeCategory memeCategory = MemeCategory.builder()
                                .meme(savedMeme)
                                .category(category)
                                .build();
                            memeCategoryRepository.save(memeCategory);
                        });
                }
            }
            
            log.info("✨ New meme added by admin: id={}, title={}, categories={}", 
                     memeId, title.trim(), categoryIds);
            redirectAttributes.addFlashAttribute("success", 
                "밈이 성공적으로 추가되었습니다! (ID: " + memeId + ")");
            
        } catch (Exception e) {
            log.error("❌ Failed to add new meme", e);
            redirectAttributes.addFlashAttribute("error", "밈 추가 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/memes";
    }

    /**
     * 밈 삭제
     */
    @PostMapping("/memes/{id}/delete")
    public String deleteMeme(@PathVariable Long id,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            if (memeRepository.existsById(id)) {
                memeRepository.deleteById(id);
                log.info("Meme deleted by admin: id={}", id);
                redirectAttributes.addFlashAttribute("success", "밈이 삭제되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("error", "존재하지 않는 밈입니다.");
            }
        } catch (Exception e) {
            log.error("Failed to delete meme: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "밈 삭제 중 오류가 발생했습니다.");
        }

        return "redirect:/admin/memes";
    }

    /**
     * 밈 수정 폼 페이지
     */
    @GetMapping("/memes/{id}/edit")
    public String editMemePage(@PathVariable Long id, Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            Meme meme = memeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("밈을 찾을 수 없습니다."));
            
            List<CategoryResponse> categories = memeLookUpService.getAllCategories();
            
            // 현재 밈의 카테고리 ID 조회
            List<Long> currentCategoryIds = memeCategoryRepository.findByMemeId(id)
                    .stream()
                    .map(memeCategory -> memeCategory.getCategory().getId())
                    .toList();
            
            model.addAttribute("meme", meme);
            model.addAttribute("categories", categories);
            model.addAttribute("currentCategoryIds", currentCategoryIds);
            
            log.info("Admin accessing edit page for meme: id={}", id);
            return "admin/edit-meme";
            
        } catch (Exception e) {
            log.error("Failed to load edit page for meme: id={}", id, e);
            return "redirect:/admin/memes";
        }
    }

    /**
     * 밈 수정 처리
     */
    @PostMapping("/memes/{id}/edit")
    public String updateMeme(@PathVariable Long id,
                           @RequestParam String title,
                           @RequestParam String origin,
                           @RequestParam String usageContext,
                           @RequestParam String hashtags,
                           @RequestParam(required = false) String imgUrl,
                           @RequestParam(required = false) MultipartFile imageFile,
                           @RequestParam(required = false) String trendPeriod,
                           @RequestParam(required = false) List<Long> categoryIds,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            Meme meme = memeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("밈을 찾을 수 없습니다."));
            
            // 이미지 URL 결정 (새 파일 업로드가 있으면 우선, 없으면 기존 또는 새 URL 사용)
            String finalImgUrl = meme.getImgUrl(); // 기존 이미지 URL을 기본값으로
            
            if (imageFile != null && !imageFile.isEmpty()) {
                log.info("📁 새 이미지 파일 업로드 시작: {}", imageFile.getOriginalFilename());
                finalImgUrl = imageUploadService.uploadImage(imageFile);
                log.info("✅ 새 이미지 파일 업로드 완료: {}", finalImgUrl);
            } else if (imgUrl != null && !imgUrl.trim().isEmpty() && !imgUrl.equals(meme.getImgUrl())) {
                finalImgUrl = imgUrl.trim();
                log.info("🔗 새 이미지 URL 사용: {}", finalImgUrl);
            }

            // 유행시기 기본값 설정
            String validTrendPeriod = (trendPeriod != null && !trendPeriod.trim().isEmpty()) 
                ? trendPeriod.trim() : meme.getTrendPeriod();
            
            // 밈 정보 업데이트
            meme.updateMeme(
                title.trim(),
                origin.trim(),
                usageContext.trim(),
                validTrendPeriod,
                finalImgUrl,
                hashtags.trim()
            );
            
            memeRepository.save(meme);
            
            // 기존 카테고리 연결 삭제
            memeCategoryRepository.deleteByMemeId(id);
            
            // 새 카테고리 연결
            if (categoryIds != null && !categoryIds.isEmpty()) {
                categoryRepository.findAllById(categoryIds)
                    .forEach(category -> {
                        MemeCategory memeCategory = MemeCategory.builder()
                            .meme(meme)
                            .category(category)
                            .build();
                        memeCategoryRepository.save(memeCategory);
                    });
            }
            
            log.info("✨ Meme updated by admin: id={}, title={}, categories={}", 
                     id, title.trim(), categoryIds);
            redirectAttributes.addFlashAttribute("success", 
                "밈이 성공적으로 수정되었습니다! (ID: " + id + ")");
            
        } catch (Exception e) {
            log.error("❌ Failed to update meme: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "밈 수정 중 오류가 발생했습니다: " + e.getMessage());
        }

        return "redirect:/admin/memes";
    }

    private boolean isAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("admin_authenticated");
        return authenticated != null && authenticated;
    }
}