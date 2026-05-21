package com.dolr.backend.controller;

import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.LoginRequest;
import com.dolr.backend.dto.LoginResponse;
import com.dolr.backend.entity.User;
import com.dolr.backend.repository.UserRepository;
import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.security.RoleCodes;
import com.dolr.backend.service.AuthService;
import com.dolr.backend.service.DesignationService;
import com.dolr.backend.service.DocumentService;
import com.dolr.backend.service.DocumentTypeService;
import com.dolr.backend.service.DivisionService;
import com.dolr.backend.service.MprService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class WebPageController {

	private final AuthService authService;
	private final AdminAuthHelper adminAuthHelper;
	private final UserRepository userRepository;
	private final DocumentService documentService;
	private final DocumentTypeService documentTypeService;
	private final DesignationService designationService;
	private final DivisionService divisionService;
	private final MprService mprService;

	@GetMapping("/")
	public String landing(HttpServletRequest request, Model model) {
		Optional<User> u = adminAuthHelper.userFromRequest(request);
		if (u.isPresent()) {
			if (RoleCodes.isPortalAdministrator(u.get())) {
				return "redirect:/home";
			}
			return "redirect:/home/documents";
		}
		model.addAttribute("pageTitle", "Institutional Memory Portal");
		model.addAttribute("headerShowLogin", true);
		return "pages/landing";
	}

	@GetMapping("/login")
	public String login(
			HttpServletRequest request,
			Model model,
			@RequestParam(required = false) String error,
			@RequestParam(required = false) String reason) {
		Optional<User> existing = adminAuthHelper.userFromRequest(request);
		if (existing.isPresent()) {
			if (RoleCodes.isPortalAdministrator(existing.get())) {
				return "redirect:/home";
			}
			return "redirect:/home/documents";
		}
		model.addAttribute("pageTitle", "Sign In");
		if (error != null) {
			model.addAttribute("loginError", loginErrorMessage(reason));
		}
		return "pages/auth";
	}

	@PostMapping("/login")
	public String loginPost(
			@RequestParam String email,
			@RequestParam String password,
			HttpServletRequest request,
			HttpServletResponse response,
			HttpSession session) {
		try {
			LoginRequest req = new LoginRequest();
			req.setEmail(email);
			req.setPassword(password);
			ApiResponse<LoginResponse> resp = authService.login(req);
			if (resp.isSuccess() && resp.getData() != null && resp.getData().getToken() != null) {
				session.setAttribute("jwt", resp.getData().getToken());
				userRepository.findByEmailIgnoreCaseWithProfile(email.trim().toLowerCase())
						.ifPresent(u -> adminAuthHelper.bindUserAfterLogin(request, response, u));
				if (Boolean.TRUE.equals(resp.getData().getPortalAdministrator())) {
					return "redirect:/home";
				}
				return "redirect:/home/documents";
			}
		} catch (RuntimeException ex) {
			return "redirect:/login?error&reason=" + loginFailureReason(ex.getMessage());
		}
		return "redirect:/login?error&reason=unknown";
	}

	private static String loginFailureReason(String message) {
		if ("User not found".equals(message)) {
			return "notfound";
		}
		if ("Invalid credentials".equals(message)) {
			return "credentials";
		}
		return "unknown";
	}

	private static String loginErrorMessage(String reason) {
		if (reason == null) {
			reason = "";
		}
		return switch (reason) {
			case "credentials" -> "Incorrect email or password.";
			case "notfound" -> "No account was found for this email. Contact your administrator if you need access.";
			default -> "Unable to sign in. Please try again.";
		};
	}

	@GetMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		if (session != null) {
			adminAuthHelper.clearWebSession(request, response, session);
			session.invalidate();
		}
		return "redirect:/login";
	}

	@GetMapping("/register")
	public String registerGone() {
		return "redirect:/login";
	}

	@PostMapping("/register")
	public String registerPostGone() {
		return "redirect:/login";
	}

	@GetMapping("/home")
	public String homeDashboard(HttpSession session, Model model) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) {
			return "redirect:/login";
		}
		User u = opt.get();
		if (!RoleCodes.isPortalAdministrator(u)) {
			return "redirect:/home/documents";
		}
		String welcomeName = u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getEmail();
		String welcomeDesignation = resolveWelcomeDesignation(u);
		model.addAttribute("welcomeName", welcomeName);
		model.addAttribute("welcomeDesignation", welcomeDesignation);
		model.addAttribute("welcomeRoleLabel", RoleCodes.isPortalAdministrator(u) ? "Administrator" : "Officer");
		model.addAttribute("welcomeInitials", initialsFromDisplayName(welcomeName));
		model.addAttribute("pageTitle", "Home");
		model.addAttribute("activeMenu", "dashboard");
		model.addAttribute("headerShowLogin", false);
		return "pages/dashboard";
	}

	private static String resolveWelcomeDesignation(User u) {
		String d = u.getDesignation();
		if (d != null && !d.isBlank()) {
			return d.trim();
		}
		if (RoleCodes.isPortalAdministrator(u)) {
			return "Administrator";
		}
		return "Not assigned";
	}

	private static String initialsFromDisplayName(String name) {
		if (name == null || name.isBlank()) {
			return "?";
		}
		String[] parts = name.trim().split("\\s+");
		if (parts.length >= 2) {
			String a = parts[0].substring(0, 1);
			String b = parts[parts.length - 1].substring(0, 1);
			return (a + b).toUpperCase();
		}
		String p = parts[0];
		return p.length() >= 2 ? p.substring(0, 2).toUpperCase() : p.substring(0, 1).toUpperCase();
	}

	@GetMapping("/home/officers")
	public String officers(HttpSession session) {
		if (adminAuthHelper.userFromSession(session).isEmpty()) {
			return "redirect:/login";
		}
		if (adminAuthHelper.isPortalAdministrator(session)) {
			return "redirect:/admin/officers";
		}
		return "redirect:/home/documents";
	}

	@GetMapping("/home/documents")
	public String documents(
			HttpSession session,
			Model model,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		if (adminAuthHelper.userFromSession(session).isEmpty()) {
			return "redirect:/login";
		}
		User viewer = adminAuthHelper.userFromSession(session).orElseThrow();
		model.addAttribute("documentPage", documentService.listVisibleForViewerPaged(viewer, page, size));
		model.addAttribute("noDesignation", documentService.isOfficerMissingDesignation(viewer.getId()));
		model.addAttribute("pageTitle", "Documents");
		model.addAttribute("activeMenu", "documents");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("documentsListPath", "/home/documents");
		model.addAttribute("documentsShowDelete", false);
		return "pages/documents";
	}

	@GetMapping("/home/documents/new")
	public String officerDocumentUploadForm(HttpSession session, Model model) {
		Optional<User> u = adminAuthHelper.userFromSession(session);
		if (u.isEmpty()) {
			return "redirect:/login";
		}
		if (RoleCodes.isPortalAdministrator(u.get())) {
			return "redirect:/admin/documents";
		}
		model.addAttribute("pageTitle", "Upload document");
		model.addAttribute("activeMenu", "documents");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("documentTypes", documentTypeService.listActiveTypes());
		model.addAttribute("designations", designationService.listActiveDesignations());
		return "pages/officer-document-upload";
	}

	@PostMapping("/home/documents/new")
	public String officerDocumentUploadPost(
			HttpSession session,
			@RequestParam String title,
			@RequestParam Long documentTypeId,
			@RequestParam(required = false) List<Long> designationIds,
			@RequestParam("file") MultipartFile file,
			RedirectAttributes ra) {
		Optional<User> u = adminAuthHelper.userFromSession(session);
		if (u.isEmpty()) {
			return "redirect:/login";
		}
		if (RoleCodes.isPortalAdministrator(u.get())) {
			return "redirect:/admin/documents";
		}
		List<Long> ids = designationIds != null ? designationIds : List.of();
		var resp = documentService.uploadPdfDocument(u.get(), title, documentTypeId, ids, file);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("uploadError", resp.getMessage());
			return "redirect:/home/documents/new";
		}
		ra.addFlashAttribute("uploadSuccess", resp.getMessage());
		return "redirect:/home/documents";
	}

	@GetMapping("/home/my-uploads")
	public String myUploads(
			HttpSession session,
			Model model,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) {
			return "redirect:/login";
		}
		User officer = opt.get();
		if (RoleCodes.isPortalAdministrator(officer)) {
			return "redirect:/admin/documents";
		}
		model.addAttribute("documentPage", documentService.listUploadedByOfficerPaged(officer, page, size));
		model.addAttribute("pageTitle", "My Uploads");
		model.addAttribute("activeMenu", "my-uploads");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("documentsListPath", "/home/my-uploads");
		model.addAttribute("documentsShowDelete", true);
		return "pages/my-uploads";
	}

	@GetMapping("/home/my-uploads/{id}/edit")
	public String myUploadsEditForm(
			HttpSession session,
			@PathVariable Long id,
			Model model) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		User officer = opt.get();
		if (RoleCodes.isPortalAdministrator(officer)) return "redirect:/admin/documents";
		var doc = documentService.getDocumentForEdit(officer, id);
		if (doc == null) {
			return "redirect:/home/my-uploads";
		}
		model.addAttribute("doc", doc);
		model.addAttribute("documentTypes", documentTypeService.listActiveTypes());
		model.addAttribute("designations", designationService.listActiveDesignations());
		model.addAttribute("selectedDesignationIds",
				doc.getVisibleToDesignations().stream()
						.map(d -> d.getId()).collect(java.util.stream.Collectors.toSet()));
		model.addAttribute("pageTitle", "Edit Document");
		model.addAttribute("activeMenu", "my-uploads");
		model.addAttribute("headerShowLogin", false);
		return "pages/officer-document-edit";
	}

	@PostMapping("/home/my-uploads/{id}/edit")
	public String myUploadsEditPost(
			HttpSession session,
			@PathVariable Long id,
			@RequestParam String title,
			@RequestParam Long documentTypeId,
			@RequestParam(required = false) List<Long> designationIds,
			RedirectAttributes ra) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		User officer = opt.get();
		if (RoleCodes.isPortalAdministrator(officer)) return "redirect:/admin/documents";
		List<Long> ids = designationIds != null ? designationIds : List.of();
		var resp = documentService.updateByOwner(officer, id, title, documentTypeId, ids);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("editError", resp.getMessage());
			return "redirect:/home/my-uploads/" + id + "/edit";
		}
		ra.addFlashAttribute("flashSuccess", "Document updated successfully.");
		return "redirect:/home/my-uploads";
	}

	@PostMapping("/home/my-uploads/delete")
	public String myUploadsDelete(
			HttpSession session,
			@RequestParam Long id,
			RedirectAttributes ra) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) {
			return "redirect:/login";
		}
		var resp = documentService.deleteByOwner(opt.get(), id);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", "Document deleted successfully.");
		}
		return "redirect:/home/my-uploads";
	}

	@GetMapping("/home/change-password")
	public String changePasswordForm(HttpSession session, Model model) {
		if (adminAuthHelper.userFromSession(session).isEmpty()) {
			return "redirect:/login";
		}
		model.addAttribute("pageTitle", "Change Password");
		model.addAttribute("activeMenu", "change-password");
		model.addAttribute("headerShowLogin", false);
		return "pages/change-password";
	}

	@PostMapping("/home/change-password")
	public String changePasswordPost(
			HttpSession session,
			@RequestParam String currentPassword,
			@RequestParam String newPassword,
			@RequestParam String confirmPassword,
			RedirectAttributes ra) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) {
			return "redirect:/login";
		}
		var result = authService.changePassword(opt.get(), currentPassword, newPassword, confirmPassword);
		if (!result.isSuccess()) {
			ra.addFlashAttribute("changePasswordError", result.getMessage());
			return "redirect:/home/change-password";
		}
		ra.addFlashAttribute("changePasswordSuccess", "Your password has been updated successfully.");
		return "redirect:/home/change-password";
	}

	// ── MPR ──────────────────────────────────────────────────────────────────

	@GetMapping("/home/mpr")
	public String mprList(
			HttpSession session, Model model,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		User officer = opt.get();
		if (RoleCodes.isPortalAdministrator(officer)) return "redirect:/home";
		model.addAttribute("mprPage", mprService.listByOfficerPaged(officer, page, size));
		model.addAttribute("pageTitle", "MPR");
		model.addAttribute("activeMenu", "mpr");
		model.addAttribute("headerShowLogin", false);
		return "pages/mpr-list";
	}

	@GetMapping("/home/mpr/new")
	public String mprUploadForm(HttpSession session, Model model) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		if (RoleCodes.isPortalAdministrator(opt.get())) return "redirect:/home";
		model.addAttribute("divisions", divisionService.listActiveDivisions());
		model.addAttribute("financialYears", MprService.financialYears());
		model.addAttribute("months", MprService.months());
		model.addAttribute("quarters", MprService.quarters());
		model.addAttribute("pageTitle", "Upload MPR");
		model.addAttribute("activeMenu", "mpr");
		model.addAttribute("headerShowLogin", false);
		return "pages/mpr-upload";
	}

	@PostMapping("/home/mpr/new")
	public String mprUploadPost(
			HttpSession session,
			@RequestParam String divisionName,
			@RequestParam String subject,
			@RequestParam String reportType,
			@RequestParam String financialYear,
			@RequestParam(required = false) String periodLabel,
			@RequestParam("files") List<MultipartFile> files,
			RedirectAttributes ra) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		User officer = opt.get();
		if (RoleCodes.isPortalAdministrator(officer)) return "redirect:/home";
		var resp = mprService.upload(officer, divisionName, subject, reportType, financialYear, periodLabel, files);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("uploadError", resp.getMessage());
			return "redirect:/home/mpr/new";
		}
		ra.addFlashAttribute("flashSuccess", "MPR uploaded successfully.");
		return "redirect:/home/mpr";
	}

	@PostMapping("/home/mpr/delete")
	public String mprDelete(HttpSession session, @RequestParam Long id, RedirectAttributes ra) {
		Optional<User> opt = adminAuthHelper.userFromSession(session);
		if (opt.isEmpty()) return "redirect:/login";
		var resp = mprService.deleteByOwner(opt.get(), id);
		if (!resp.isSuccess()) ra.addFlashAttribute("flashError", resp.getMessage());
		else ra.addFlashAttribute("flashSuccess", "MPR deleted.");
		return "redirect:/home/mpr";
	}

	@GetMapping("/home/reports")
	public String reports(HttpSession session) {
		if (adminAuthHelper.userFromSession(session).isEmpty()) {
			return "redirect:/login";
		}
		if (adminAuthHelper.isPortalAdministrator(session)) {
			return "redirect:/admin/documents";
		}
		return "redirect:/home/documents";
	}

	@GetMapping("/home/settings")
	public String settings(HttpSession session) {
		if (adminAuthHelper.userFromSession(session).isEmpty()) {
			return "redirect:/login";
		}
		if (adminAuthHelper.isPortalAdministrator(session)) {
			return "redirect:/admin/documents";
		}
		return "redirect:/home/documents";
	}
}
