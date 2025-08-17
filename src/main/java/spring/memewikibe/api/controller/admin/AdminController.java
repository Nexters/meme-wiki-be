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
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final MemeRepository memeRepository;
    private final ImageUploadService imageUploadService;

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

        List<Meme> memes = memeRepository.findAllByOrderByIdDesc();
        model.addAttribute("memes", memes);
        model.addAttribute("totalCount", memes.size());
        
        log.info("Admin accessing memes page. Total memes: {}", memes.size());
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

            Meme meme = Meme.builder()
                    .title(title.trim())
                    .origin(origin.trim())
                    .usageContext(usageContext.trim())
                    .hashtags(hashtags.trim())
                    .imgUrl(finalImgUrl)
                    .trendPeriod(trendPeriod != null ? trendPeriod.trim() : null)
                    .build();

            Meme savedMeme = memeRepository.save(meme);
            
            log.info("✨ New meme added by admin: id={}, title={}, imgUrl={}", 
                     savedMeme.getId(), savedMeme.getTitle(), savedMeme.getImgUrl());
            redirectAttributes.addFlashAttribute("success", 
                "밈이 성공적으로 추가되었습니다! (ID: " + savedMeme.getId() + ")");
            
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

    private boolean isAuthenticated(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("admin_authenticated");
        return authenticated != null && authenticated;
    }
}