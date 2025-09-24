package spring.memewikibe.api.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


import spring.memewikibe.domain.meme.Meme;
import spring.memewikibe.infrastructure.MemeRepository;
import spring.memewikibe.application.ImageUploadService;
import spring.memewikibe.application.MemeLookUpService;
import spring.memewikibe.application.MemeCreateService;
import spring.memewikibe.infrastructure.ai.MemeVectorIndexService;
import spring.memewikibe.api.controller.meme.response.CategoryResponse;
import spring.memewikibe.api.controller.meme.request.MemeCreateRequest;

import spring.memewikibe.domain.meme.MemeCategory;
import spring.memewikibe.infrastructure.CategoryRepository;
import spring.memewikibe.infrastructure.MemeCategoryRepository;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
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
    private final MemeVectorIndexService vectorIndexService;

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
    public String memesPage(@RequestParam(name = "showApprovedOnly", defaultValue = "true") boolean showApprovedOnly,
                           Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        List<Object[]> memeWithCategories;
        List<Meme> memes;
        
        if (showApprovedOnly) {
            // 승인된 밈만 조회
            memeWithCategories = memeRepository.findByFlagWithCategoryNamesOrderByIdDesc(Meme.Flag.NORMAL);
            memes = memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
        } else {
            // 모든 밈 조회
            memeWithCategories = memeRepository.findAllWithCategoryNamesOrderByIdDesc();
            memes = memeRepository.findAllByOrderByIdDesc();
        }
        
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
        model.addAttribute("showApprovedOnly", showApprovedOnly);
        
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
     * 밈 일괄 삭제
     */
    @PostMapping("/memes/delete-multiple")
    @Transactional
    public String deleteMultipleMemes(@RequestParam("memeIds") String memeIdsString,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        try {
            if (memeIdsString == null || memeIdsString.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "삭제할 밈을 선택해주세요.");
                return "redirect:/admin/memes";
            }

            // 문자열을 파싱해서 Long 리스트로 변환
            List<Long> memeIds;
            try {
                memeIds = Arrays.stream(memeIdsString.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::valueOf)
                        .toList();
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("error", "잘못된 밈 ID 형식입니다.");
                return "redirect:/admin/memes";
            }

            if (memeIds.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "삭제할 밈을 선택해주세요.");
                return "redirect:/admin/memes";
            }

            memeRepository.deleteByIdIn(memeIds);
            log.info("Memes deleted by admin: ids={}", memeIds);
            redirectAttributes.addFlashAttribute("success", 
                    memeIds.size() + "개의 밈이 삭제되었습니다.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "밈 일괄 삭제 중 오류가 발생했습니다.");
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

            // Reindex in vector store
            try {
                vectorIndexService.reindex(meme);
            } catch (Exception ignored) {}
            
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
    
    /**
     * 밈 수정 후 바로 승인 처리
     */
    @PostMapping("/memes/{id}/edit-and-approve")
    public String editAndApproveMeme(@PathVariable Long id,
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
            log.info("✏️ Editing and approving meme: id={}", id);
            
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
            
            // 승인 처리
            meme.approve();

            // 카테고리 업데이트
            memeCategoryRepository.deleteByMemeId(meme.getId());
            if (categoryIds != null && !categoryIds.isEmpty()) {
                categoryRepository.findAllById(categoryIds)
                    .forEach(category -> {
                        MemeCategory memeCategory = MemeCategory.create(meme, category);
                        memeCategoryRepository.save(memeCategory);
                    });
            }

            memeRepository.save(meme);

            // Reindex after approve
            try {
                vectorIndexService.reindex(meme);
            } catch (Exception ignored) {}
            
            log.info("✅ Meme edited and approved successfully: id={}, title={}, categories={}", 
                     id, title.trim(), categoryIds);
            redirectAttributes.addFlashAttribute("success", "밈이 수정되고 승인되어 전체 사용자에게 공개되었습니다!");
            
            return "redirect:/admin/memes/review";
            
        } catch (Exception e) {
            log.error("❌ Failed to edit and approve meme: id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "밈 수정 및 승인 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/admin/memes/" + id + "/edit";
        }
    }
    
    /**
     * 검토 대기 중인 밈 (ABNORMAL) 관리 페이지
     */
    @GetMapping("/memes/review")
    public String reviewMemesPage(Model model, HttpSession session) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        // ABNORMAL 상태의 밈들만 조회
        List<Object[]> abnormalMemesWithCategories = memeRepository.findByFlagWithCategoryNamesOrderByIdDesc(Meme.Flag.ABNORMAL);
        
        // 밈과 카테고리 정보 매핑
        Map<Long, List<String>> memeCategoryMap = new HashMap<>();
        List<Meme> abnormalMemes = new ArrayList<>();
        
        for (Object[] result : abnormalMemesWithCategories) {
            Meme meme = (Meme) result[0];
            String categoryName = (String) result[1];
            
            if (abnormalMemes.stream().noneMatch(m -> m.getId().equals(meme.getId()))) {
                abnormalMemes.add(meme);
            }
            
            if (categoryName != null) {
                memeCategoryMap.computeIfAbsent(meme.getId(), k -> new ArrayList<>()).add(categoryName);
            }
        }
        
        // 통계 정보
        long abnormalCount = memeRepository.countByFlag(Meme.Flag.ABNORMAL);
        long normalCount = memeRepository.countByFlag(Meme.Flag.NORMAL);
        
        model.addAttribute("abnormalMemes", abnormalMemes);
        model.addAttribute("memeCategoryMap", memeCategoryMap);
        model.addAttribute("abnormalCount", abnormalCount);
        model.addAttribute("normalCount", normalCount);
        
        return "admin/review-memes";
    }
    
    /**
     * 밈 승인 (ABNORMAL → NORMAL)
     */
    @PostMapping("/memes/{id}/approve")
    @ResponseBody
    public ResponseEntity<String> approveMeme(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }
        
        try {
            Meme meme = memeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("밈을 찾을 수 없습니다."));
            
            meme.approve();
            memeRepository.save(meme);
            
            return ResponseEntity.ok("밈이 승인되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("승인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 밈 반려 (NORMAL → ABNORMAL)
     */
    @PostMapping("/memes/{id}/reject")
    @ResponseBody
    public ResponseEntity<String> rejectMeme(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }
        
        try {
            Meme meme = memeRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("밈을 찾을 수 없습니다."));
            
            meme.reject();
            memeRepository.save(meme);
            
            return ResponseEntity.ok("밈이 반려되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("반려 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모든 승인된(NORMAL) 밈을 벡터 스토어로 일괄 업서트합니다.
     * - Pinecone 설정이 없으면 서비스 내부에서 경고를 남기고 스킵됩니다.
     * - 대량 데이터 고려해 batchSize 단위로 업서트합니다.
     */
    @PostMapping("/memes/reindex-vectors")
    public String reindexAllApprovedMemes(HttpSession session, RedirectAttributes redirectAttributes,
                                          @RequestParam(name = "batchSize", required = false, defaultValue = "100") int batchSize) {
        if (!isAuthenticated(session)) {
            return "redirect:/admin/login";
        }
        try {
            List<Meme> approved = memeRepository.findByFlagOrderByIdDesc(Meme.Flag.NORMAL);
            int total = approved.size();
            if (total == 0) {
                redirectAttributes.addFlashAttribute("info", "승인된 밈이 없습니다.");
                return "redirect:/admin/memes";
            }
            if (batchSize <= 0) batchSize = 100;

            int batches = 0;
            for (int start = 0; start < total; start += batchSize) {
                int end = Math.min(start + batchSize, total);
                List<Meme> chunk = approved.subList(start, end);
                try {
                    vectorIndexService.upsertVectors(chunk);
                } catch (Exception e) {
                    log.warn("일부 배치 업서트 실패: {}-{}: {}", start, end, e.toString());
                }
                batches++;
            }
            log.info("✅ Reindex completed. total={}, batchSize={}, batches={}", total, batchSize, batches);
            redirectAttributes.addFlashAttribute("success", "벡터 인덱스 재구성 완료: 총 " + total + "건, 배치 " + batches + "개");
        } catch (Exception e) {
            log.error("❌ Reindex failed", e);
            redirectAttributes.addFlashAttribute("error", "벡터 인덱스 재구성 중 오류: " + e.getMessage());
        }
        return "redirect:/admin/memes";
    }

    private boolean isAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("admin_authenticated");
        return authenticated != null && authenticated;
    }
}