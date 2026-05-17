package com.dolr.backend.controller;

import com.dolr.backend.dto.AdminCreateOfficerRequest;
import com.dolr.backend.dto.AdminEditOfficerRequest;
import com.dolr.backend.dto.ApiResponse;
import com.dolr.backend.dto.CreateDocumentTypeRequest;
import com.dolr.backend.dto.CreateDesignationRequest;
import com.dolr.backend.dto.CreateDivisionRequest;
import com.dolr.backend.dto.DocumentTablePage;
import com.dolr.backend.dto.UpdateDesignationRequest;
import com.dolr.backend.dto.UpdateDivisionRequest;
import com.dolr.backend.dto.UpdateOfficerRequest;
import com.dolr.backend.entity.User;
import com.dolr.backend.security.AdminAuthHelper;
import com.dolr.backend.security.RoleCodes;
import com.dolr.backend.service.AuthService;
import com.dolr.backend.service.DesignationService;
import com.dolr.backend.service.DocumentService;
import com.dolr.backend.service.DocumentTypeService;
import com.dolr.backend.service.DivisionService;
import com.dolr.backend.service.OfficerService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.beans.PropertyEditorSupport;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class AdminWebController {

	private final AdminAuthHelper adminAuthHelper;
	private final DivisionService divisionService;
	private final DesignationService designationService;
	private final AuthService authService;
	private final OfficerService officerService;
	private final DocumentTypeService documentTypeService;
	private final DocumentService documentService;

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		binder.registerCustomEditor(Long.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (text == null || text.isBlank()) {
					setValue(null);
				} else {
					setValue(Long.parseLong(text.trim()));
				}
			}
		});
		binder.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
			@Override
			public void setAsText(String text) throws IllegalArgumentException {
				if (text == null || text.isBlank()) {
					setValue(null);
				} else {
					setValue(Integer.parseInt(text.trim()));
				}
			}
		});
	}

	private static Optional<User> adminUser(HttpSession session, AdminAuthHelper helper) {
		return helper.userFromSession(session).filter(RoleCodes::isPortalAdministrator);
	}

	private static String redirectUnlessAdmin(HttpSession session, AdminAuthHelper helper) {
		Optional<User> u = helper.userFromSession(session);
		if (u.isEmpty()) {
			return "redirect:/login";
		}
		if (!RoleCodes.isPortalAdministrator(u.get())) {
			return "redirect:/home/documents";
		}
		return null;
	}

	private static Long parseOptionalLong(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(raw.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static LocalDate parseOptionalLocalDate(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(raw.trim());
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	private static UpdateOfficerRequest toUpdateOfficerRequest(AdminEditOfficerRequest f) {
		UpdateOfficerRequest r = new UpdateOfficerRequest();
		r.setFullName(f.getFullName().trim());
		r.setEmail(f.getEmail().trim());
		r.setMobileNumber(f.getMobileNumber().trim());
		r.setDepartment(f.getDepartment() != null ? f.getDepartment().trim() : null);
		r.setDesignationId(f.getDesignationId());
		r.setDivisionId(f.getDivisionId());
		r.setReportingOfficerId(f.getReportingOfficerId());
		r.setRole("OFFICER");
		return r;
	}

	@GetMapping("/admin")
	public String hub(HttpSession session) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		return "redirect:/admin/documents";
	}

	@GetMapping("/admin/divisions")
	public String divisions(HttpSession session, Model model) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		model.addAttribute("pageTitle", "Divisions");
		model.addAttribute("activeMenu", "admin-divisions");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("divisions", divisionService.listAllDivisionsAsAdmin());
		model.addAttribute("newDivision", new CreateDivisionRequest());
		return "pages/admin/divisions";
	}

	@PostMapping("/admin/divisions/add")
	public String divisionsAdd(
			HttpSession session,
			@Valid @ModelAttribute("newDivision") CreateDivisionRequest req,
			BindingResult br,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (br.hasErrors()) {
			ra.addFlashAttribute("flashError", br.getFieldError() != null ? br.getFieldError().getDefaultMessage() : "Invalid input");
			return "redirect:/admin/divisions";
		}
		var resp = divisionService.createDivisionAsAdmin(req);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
			return "redirect:/admin/divisions";
		}
		ra.addFlashAttribute("flashSuccess", "Division created.");
		return "redirect:/admin/divisions";
	}

	@PostMapping("/admin/divisions/update")
	public String divisionsUpdate(
			HttpSession session,
			@RequestParam Long id,
			@RequestParam String name,
			@RequestParam(required = false) Integer sortOrder,
			@RequestParam(defaultValue = "false") boolean active,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		UpdateDivisionRequest req = new UpdateDivisionRequest();
		req.setName(name);
		req.setSortOrder(sortOrder);
		req.setActive(active);
		var resp = divisionService.updateDivisionAsAdmin(id, req);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", "Division updated.");
		}
		return "redirect:/admin/divisions";
	}

	@PostMapping("/admin/divisions/deactivate")
	public String divisionsDeactivate(
			HttpSession session,
			@RequestParam Long id,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		var resp = divisionService.deactivateDivisionAsAdmin(id);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", resp.getMessage());
		}
		return "redirect:/admin/divisions";
	}

	@GetMapping("/admin/designations")
	public String designations(HttpSession session, Model model) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		model.addAttribute("pageTitle", "Designations");
		model.addAttribute("activeMenu", "admin-designations");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("designations", designationService.listAllAsAdmin());
		model.addAttribute("newDesignation", new CreateDesignationRequest());
		return "pages/admin/designations";
	}

	@PostMapping("/admin/designations/add")
	public String designationsAdd(
			HttpSession session,
			@Valid @ModelAttribute("newDesignation") CreateDesignationRequest req,
			BindingResult br,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (br.hasErrors()) {
			ra.addFlashAttribute("flashError", br.getFieldError() != null ? br.getFieldError().getDefaultMessage() : "Invalid input");
			return "redirect:/admin/designations";
		}
		var resp = designationService.createAsAdmin(req);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
			return "redirect:/admin/designations";
		}
		ra.addFlashAttribute("flashSuccess", "Designation created.");
		return "redirect:/admin/designations";
	}

	@PostMapping("/admin/designations/update")
	public String designationsUpdate(
			HttpSession session,
			@RequestParam Long id,
			@RequestParam String name,
			@RequestParam(required = false) Integer sortOrder,
			@RequestParam(defaultValue = "false") boolean handlesAllDivisions,
			@RequestParam(defaultValue = "false") boolean active,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		UpdateDesignationRequest req = new UpdateDesignationRequest();
		req.setName(name);
		req.setSortOrder(sortOrder);
		req.setHandlesAllDivisions(handlesAllDivisions);
		req.setActive(active);
		var resp = designationService.updateAsAdmin(id, req);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", "Designation updated.");
		}
		return "redirect:/admin/designations";
	}

	@PostMapping("/admin/designations/deactivate")
	public String designationsDeactivate(
			HttpSession session,
			@RequestParam Long id,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		var resp = designationService.deactivateAsAdmin(id);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", resp.getMessage());
		}
		return "redirect:/admin/designations";
	}

	@GetMapping("/admin/employees")
	public String employeesLegacyRedirect(HttpSession session) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		return "redirect:/admin/officers";
	}

	private void populateOfficerForm(Model model) {
		model.addAttribute("divisions", divisionService.listActiveDivisions());
		model.addAttribute("designations", designationService.listActiveDesignations());
		model.addAttribute("reportingOfficers", officerService.getAllOfficers());
	}

	@GetMapping("/admin/officers/new")
	public String officerNewForm(HttpSession session, Model model) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		model.addAttribute("pageTitle", "Add officer");
		model.addAttribute("activeMenu", "admin-officer-new");
		model.addAttribute("headerShowLogin", false);
		var desList = designationService.listActiveDesignations();
		var divList = divisionService.listActiveDivisions();
		AdminCreateOfficerRequest o = new AdminCreateOfficerRequest();
		if (!desList.isEmpty()) {
			o.setDesignationId(desList.get(0).getId());
		}
		if (!divList.isEmpty()) {
			o.setDivisionId(divList.get(0).getId());
		}
		model.addAttribute("officer", o);
		populateOfficerForm(model);
		return "pages/admin/officer-new";
	}

	@PostMapping("/admin/officers/new")
	public String officerCreate(
			HttpSession session,
			Model model,
			RedirectAttributes ra,
			@Valid @ModelAttribute("officer") AdminCreateOfficerRequest officer,
			BindingResult br) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (br.hasErrors()) {
			model.addAttribute("pageTitle", "Add officer");
			model.addAttribute("activeMenu", "admin-officer-new");
			model.addAttribute("headerShowLogin", false);
			populateOfficerForm(model);
			return "pages/admin/officer-new";
		}
		User admin = adminUser(session, adminAuthHelper).orElse(null);
		ApiResponse<Void> resp = authService.createOfficerByAdmin(admin, officer);
		if (!resp.isSuccess()) {
			model.addAttribute("pageTitle", "Add officer");
			model.addAttribute("activeMenu", "admin-officer-new");
			model.addAttribute("headerShowLogin", false);
			model.addAttribute("formError", resp.getMessage());
			populateOfficerForm(model);
			return "pages/admin/officer-new";
		}
		ra.addFlashAttribute("flashSuccess", resp.getMessage());
		return "redirect:/admin/officers";
	}

	@GetMapping("/admin/officers")
	public String officersList(HttpSession session, Model model) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		model.addAttribute("pageTitle", "Officers");
		model.addAttribute("activeMenu", "admin-officers");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("officers", officerService.getAllOfficers());
		return "pages/admin/officers-list";
	}

	@GetMapping("/admin/officers/{id}/edit")
	public String officerEditForm(HttpSession session, Model model, @PathVariable long id) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		User u = officerService.findEditableOfficerForAdmin(id).orElse(null);
		if (u == null) {
			return "redirect:/admin/officers";
		}
		AdminEditOfficerRequest form = new AdminEditOfficerRequest();
		form.setFullName(u.getFullName());
		form.setEmail(u.getEmail());
		form.setMobileNumber(u.getMobileNumber() != null ? u.getMobileNumber() : "");
		form.setDepartment(u.getDepartment());
		if (u.getDesignationRef() != null) {
			form.setDesignationId(u.getDesignationRef().getId());
		}
		if (u.getDivisionRef() != null) {
			form.setDivisionId(u.getDivisionRef().getId());
		}
		if (u.getReportingOfficer() != null) {
			form.setReportingOfficerId(u.getReportingOfficer().getId());
		}
		model.addAttribute("pageTitle", "Edit officer");
		model.addAttribute("activeMenu", "admin-officer-edit");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("officerId", id);
		model.addAttribute("officerEdit", form);
		populateOfficerForm(model);
		return "pages/admin/officer-edit";
	}

	@PostMapping("/admin/officers/{id}/edit")
	public String officerEditSave(
			HttpSession session,
			@PathVariable long id,
			Model model,
			RedirectAttributes ra,
			@Valid @ModelAttribute("officerEdit") AdminEditOfficerRequest form,
			BindingResult br) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (officerService.findEditableOfficerForAdmin(id).isEmpty()) {
			return "redirect:/admin/officers";
		}
		if (br.hasErrors()) {
			model.addAttribute("pageTitle", "Edit officer");
			model.addAttribute("activeMenu", "admin-officer-edit");
			model.addAttribute("headerShowLogin", false);
			model.addAttribute("officerId", id);
			populateOfficerForm(model);
			return "pages/admin/officer-edit";
		}
		try {
			officerService.updateOfficer(id, toUpdateOfficerRequest(form));
		} catch (RuntimeException ex) {
			model.addAttribute("pageTitle", "Edit officer");
			model.addAttribute("activeMenu", "admin-officer-edit");
			model.addAttribute("headerShowLogin", false);
			model.addAttribute("officerId", id);
			model.addAttribute("formError", ex.getMessage());
			populateOfficerForm(model);
			return "pages/admin/officer-edit";
		}
		ra.addFlashAttribute("flashSuccess", "Officer updated.");
		return "redirect:/admin/officers";
	}

	@PostMapping("/admin/officers/{id}/delete")
	public String officerDelete(HttpSession session, @PathVariable long id, RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (officerService.findEditableOfficerForAdmin(id).isEmpty()) {
			ra.addFlashAttribute("flashError", "Officer not found.");
			return "redirect:/admin/officers";
		}
		try {
			officerService.deleteOfficer(id);
		} catch (RuntimeException ex) {
			ra.addFlashAttribute("flashError", ex.getMessage());
			return "redirect:/admin/officers";
		}
		ra.addFlashAttribute("flashSuccess", "Officer access removed.");
		return "redirect:/admin/officers";
	}

	@GetMapping("/admin/document-types")
	public String documentTypes(HttpSession session, Model model) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		model.addAttribute("pageTitle", "Document types");
		model.addAttribute("activeMenu", "admin-document-types");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("documentTypes", documentTypeService.listAllAsAdmin());
		model.addAttribute("newDocumentType", new CreateDocumentTypeRequest());
		return "pages/admin/document-types";
	}

	@PostMapping("/admin/document-types/add")
	public String documentTypesAdd(
			HttpSession session,
			@Valid @ModelAttribute("newDocumentType") CreateDocumentTypeRequest req,
			BindingResult br,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		if (br.hasErrors()) {
			ra.addFlashAttribute("flashError", br.getFieldError() != null ? br.getFieldError().getDefaultMessage() : "Invalid input");
			return "redirect:/admin/document-types";
		}
		var resp = documentTypeService.createAsAdmin(req);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
			return "redirect:/admin/document-types";
		}
		ra.addFlashAttribute("flashSuccess", "Document type created.");
		return "redirect:/admin/document-types";
	}

	@PostMapping("/admin/document-types/deactivate")
	public String documentTypesDeactivate(
			HttpSession session,
			@RequestParam Long id,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		var resp = documentTypeService.deactivateAsAdmin(id);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", resp.getMessage());
		}
		return "redirect:/admin/document-types";
	}

	@GetMapping("/admin/documents")
	public String adminDocumentsList(
			HttpSession session,
			Model model,
			@RequestParam(name = "dateFrom", required = false) String dateFrom,
			@RequestParam(name = "dateTo", required = false) String dateTo,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "10") int size) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		LocalDate fromD = parseOptionalLocalDate(dateFrom);
		LocalDate toD = parseOptionalLocalDate(dateTo);
		boolean filterRequested = (dateFrom != null && !dateFrom.isBlank())
				|| (dateTo != null && !dateTo.isBlank());
		boolean filterInvalid = filterRequested && fromD == null && toD == null;
		model.addAttribute("pageTitle", "Published documents");
		model.addAttribute("activeMenu", "admin-documents");
		model.addAttribute("headerShowLogin", false);
		model.addAttribute("filterFrom", dateFrom != null ? dateFrom.trim() : "");
		model.addAttribute("filterTo", dateTo != null ? dateTo.trim() : "");
		model.addAttribute("filterInvalid", filterInvalid);
		model.addAttribute("documentPage", filterInvalid
				? DocumentTablePage.builder()
						.content(List.of())
						.page(0)
						.totalPages(0)
						.totalElements(0)
						.size(size)
						.hasPrevious(false)
						.hasNext(false)
						.build()
				: documentService.listAllForAdminListingPaged(page, size, fromD, toD));
		model.addAttribute("documentsListPath", "/admin/documents");
		model.addAttribute("documentsShowDelete", true);
		return "pages/admin/documents-list";
	}

	/** Legacy URL: uploads are officer-only from /home/documents/new */
	@GetMapping("/admin/documents/new")
	public String adminUploadRedirect(HttpSession session) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		return "redirect:/admin/documents";
	}

	@PostMapping("/admin/documents/delete")
	public String adminDocumentDelete(
			HttpSession session,
			@RequestParam Long id,
			RedirectAttributes ra) {
		String deny = redirectUnlessAdmin(session, adminAuthHelper);
		if (deny != null) {
			return deny;
		}
		User admin = adminUser(session, adminAuthHelper).orElse(null);
		var resp = documentService.deleteByAdmin(admin, id);
		if (!resp.isSuccess()) {
			ra.addFlashAttribute("flashError", resp.getMessage());
		} else {
			ra.addFlashAttribute("flashSuccess", resp.getMessage());
		}
		return "redirect:/admin/documents";
	}
}
