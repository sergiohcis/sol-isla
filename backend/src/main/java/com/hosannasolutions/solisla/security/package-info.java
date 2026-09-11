/** Admin authentication (session-cookie, CSRF-protected) and permission-based authorization.
 *  See CLAUDE.md rule 9 — Angular route guards are UX only, every admin endpoint independently
 *  checks permissions server-side via {@code @PreAuthorize}. */
package com.hosannasolutions.solisla.security;
