package com.dolr.backend.config;

import com.dolr.backend.controller.WebPageController;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Maps multipart parsing failures (including oversize uploads) to a flash message on the
 * officer upload page instead of a generic error page.
 */
@ControllerAdvice(assignableTypes = WebPageController.class)
public class MultipartUploadExceptionAdvice {

	@Value("${spring.servlet.multipart.max-file-size:30MB}")
	private String maxFileSizeHuman;

	@ExceptionHandler(MultipartException.class)
	public String onMultipart(MultipartException ex, HttpServletRequest req, RedirectAttributes ra) {
		if (ex instanceof MaxUploadSizeExceededException) {
			ra.addFlashAttribute("uploadError",
					"Upload rejected: file or total request exceeds the server limit (" + maxFileSizeHuman + " per file). Try a smaller PDF or ask your administrator to raise the limit.");
		} else {
			ra.addFlashAttribute("uploadError",
					"Upload failed: the server could not read the file. Check size and connection, then try again.");
		}
		return redirectForUpload(req);
	}

	private static String redirectForUpload(HttpServletRequest req) {
		String uri = req.getRequestURI();
		if (uri != null && uri.contains("/home/documents/new")) {
			return "redirect:/home/documents/new";
		}
		return "redirect:/home/documents";
	}
}
