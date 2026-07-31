# Specification: core-notification

## 1. Overview

`core-notification` is the notification dispatch engine for the AttendAI platform. It provides a unified interface for sending notifications across multiple channels — email, push, and in-app — without business modules needing to know how each channel is implemented.

Any Core module or business module dispatches a notification by calling the `NotificationService` Spring bean with a notification request. The notification module handles channel selection, template rendering, delivery scheduling, retry on failure, and delivery status tracking.

`core-notification` is fully domain-agnostic. It does not know whether a notification is about school attendance, an employee shift, or a system alert. It knows only that a notification must be sent to a recipient via one or more channels.

---

## 2. Scope and Objectives

**In scope:**
- Sending email notifications (SMTP or provider API)
- Sending in-app notifications (persisted, queryable by the recipient)
- Push notification dispatch (FCM or provider API, V1 optional)
- Notification template management (create, update, render with variable substitution)
- Notification log (delivery history, status, retry tracking)
- Notification preference management (user opt-in/opt-out per channel and notification type)
- Scheduled / delayed notification support
- Retry on delivery failure (configurable max retries with exponential backoff)

**Out of scope:**
- SMS notifications (V1 excluded; interface designed to accommodate it later)
- Email template design (HTML/text templates are managed as text records)
- Transactional email provider selection and credentials (configuration concern)
- Business-domain notification content (notification types and template bodies are defined by each module)

---

## 3. Functional Requirements

### FR-NOTIF-01: Send Notification
Accept a notification dispatch request containing: recipient user ID, notification type code, channel(s), template variables, and optional scheduled send time. The module resolves the template, renders the content, checks user preferences, and dispatches via the appropriate channel.

### FR-NOTIF-02: Notification Templates
Define named notification templates per channel. A template has a type code (e.g., `AUTH_PASSWORD_RESET`), a channel, a subject (for email), and a body with Mustache-style variable placeholders (`{{variableName}}`).

### FR-NOTIF-03: Template Rendering
Render a template by substituting variable placeholders with values provided in the notification request. Rendering must not fail silently — missing required variables produce a template rendering error.

### FR-NOTIF-04: User Notification Preferences
Users can opt out of specific notification types per channel. If a user has opted out of a notification type on a given channel, the notification is not sent on that channel (but is still logged as SKIPPED).

### FR-NOTIF-05: In-App Notification Inbox
Persist in-app notifications to the database so the recipient can query their notification inbox. In-app notifications have a `read` / `unread` state.

### FR-NOTIF-06: Mark Notification as Read
Allow the recipient to mark an in-app notification as read.

### FR-NOTIF-07: Get Notification Inbox
Return a paginated list of in-app notifications for the current authenticated user, ordered by `createdAt` descending.

### FR-NOTIF-08: Delivery Status Tracking
Persist a delivery log entry for every notification dispatch attempt. Track: channel, status (PENDING, SENT, FAILED, SKIPPED), attempt count, last error.

### FR-NOTIF-09: Retry Failed Notifications
A scheduled job retries FAILED notification dispatches up to a configurable maximum number of attempts using exponential backoff. After max retries, status is set to PERMANENTLY_FAILED.

### FR-NOTIF-10: Notification History Query
An admin can query the notification delivery log: filter by user, type, status, channel, date range.

---

## 4. Non-Functional Requirements

- Email dispatch must be non-blocking. Notifications are queued or sent asynchronously; the calling service must not wait for SMTP delivery.
- In-app notification persistence is synchronous (written to DB in the calling thread).
- Notification dispatch must not throw an uncaught exception that propagates to the caller. Failures are logged and retried.
- The retry job runs every 5 minutes (configurable).
- Maximum retry attempts: 3 (configurable).
- The notification inbox query must respond in under 100ms for up to 1,000 unread notifications.

---

## 5. Business Rules

- BR-NOTIF-01: If a user has opted out of a notification type on a channel, the notification is skipped on that channel. Other channels are unaffected.
- BR-NOTIF-02: A notification with no valid channel (all opted out or no template exists for the channel) is logged as SKIPPED with a reason.
- BR-NOTIF-03: A template must exist for the requested type+channel combination. If no template exists, the dispatch is logged as FAILED.
- BR-NOTIF-04: Notification dispatch failures must never propagate to the caller. The service catches all delivery exceptions internally.
- BR-NOTIF-05: In-app notifications are always persisted regardless of email/push success or failure.
- BR-NOTIF-06: Scheduled notifications are stored in `PENDING` status until the scheduled time arrives.
- BR-NOTIF-07: Template variable substitution is strict: all declared required variables must be present in the request. Optional variables default to empty string.

---

## 6. Domain Model

### NotificationTemplate Entity

| Field       | Type          | Description                                              |
|-------------|---------------|----------------------------------------------------------|
| id          | Long          | Surrogate PK                                             |
| typeCode    | String        | Notification type, e.g. `AUTH_PASSWORD_RESET`, max 100  |
| channel     | Channel       | Enum: EMAIL, IN_APP, PUSH                                |
| locale      | String        | Locale code, e.g. `en`, `en-IN`, default `en`, max 10   |
| subject     | String        | Email subject (nullable for IN_APP, PUSH), max 255       |
| bodyTemplate| String        | Template body with `{{variable}}` placeholders (TEXT)   |
| isActive    | boolean       | Whether this template is in use                          |
| isDeleted   | boolean       | Soft delete                                              |
| deletedAt   | LocalDateTime | Soft delete timestamp                                    |
| createdAt   | LocalDateTime | Audit                                                    |
| updatedAt   | LocalDateTime | Audit                                                    |
| createdBy   | Long          | Audit                                                    |
| updatedBy   | Long          | Audit                                                    |

### NotificationLog Entity

| Field          | Type               | Description                                           |
|----------------|--------------------|-------------------------------------------------------|
| id             | Long               | Surrogate PK                                          |
| recipientUserId| Long               | FK → users(id), NOT NULL                              |
| typeCode       | String             | Notification type code, max 100                       |
| channel        | Channel            | Channel used                                          |
| status         | NotificationStatus | Enum: PENDING, SENT, FAILED, SKIPPED, PERMANENTLY_FAILED |
| subject        | String             | Rendered subject (for email), max 255                 |
| renderedBody   | String             | Rendered notification body (TEXT)                     |
| errorMessage   | String             | Last error message if FAILED, max 1000                |
| attemptCount   | int                | Number of dispatch attempts                           |
| scheduledAt    | LocalDateTime      | When to send (null = send immediately)                |
| sentAt         | LocalDateTime      | When successfully sent                                |
| createdAt      | LocalDateTime      | Audit                                                 |
| updatedAt      | LocalDateTime      | Audit                                                 |

### InAppNotification Entity

| Field          | Type          | Description                                          |
|----------------|---------------|------------------------------------------------------|
| id             | Long          | Surrogate PK                                         |
| recipientUserId| Long          | FK → users(id), NOT NULL                             |
| typeCode       | String        | Notification type, max 100                           |
| title          | String        | Notification title, max 255                          |
| body           | String        | Notification body (TEXT)                             |
| isRead         | boolean       | Read flag                                            |
| readAt         | LocalDateTime | When marked read, nullable                           |
| createdAt      | LocalDateTime | Audit                                                |
| updatedAt      | LocalDateTime | Audit                                                |

### NotificationPreference Entity

| Field      | Type      | Description                                       |
|------------|-----------|---------------------------------------------------|
| id         | Long      | Surrogate PK                                      |
| userId     | Long      | FK → users(id), NOT NULL                          |
| typeCode   | String    | Notification type, max 100                        |
| channel    | Channel   | Channel                                           |
| isEnabled  | boolean   | true = opted in, false = opted out                |
| createdAt  | LocalDateTime | Audit                                         |
| updatedAt  | LocalDateTime | Audit                                         |

---

## 7. Channel Enum
- `EMAIL`
- `IN_APP`
- `PUSH`

## NotificationStatus Enum
- `PENDING` — queued for dispatch (including scheduled)
- `SENT` — successfully delivered
- `FAILED` — delivery attempt failed, will retry
- `SKIPPED` — skipped due to user preference or missing template
- `PERMANENTLY_FAILED` — max retries exhausted

---

## 8. Data Model

### Table: `notification_templates`

```sql
CREATE TABLE notification_templates (
    id            BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    type_code     VARCHAR(100)     NOT NULL,
    channel       VARCHAR(20)      NOT NULL,
    locale        VARCHAR(10)      NOT NULL DEFAULT 'en',
    subject       VARCHAR(255)     NULL,
    body_template TEXT             NOT NULL,
    is_active     BOOLEAN          NOT NULL DEFAULT TRUE,
    is_deleted    BOOLEAN          NOT NULL DEFAULT FALSE,
    deleted_at    DATETIME         NULL,
    created_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    BIGINT UNSIGNED  NULL,
    updated_by    BIGINT UNSIGNED  NULL,

    UNIQUE uq_notification_templates (type_code, channel, locale),
    INDEX idx_notification_templates_type_code (type_code)
);
```

### Table: `notification_logs`

```sql
CREATE TABLE notification_logs (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT UNSIGNED  NOT NULL,
    type_code         VARCHAR(100)     NOT NULL,
    channel           VARCHAR(20)      NOT NULL,
    status            VARCHAR(30)      NOT NULL DEFAULT 'PENDING',
    subject           VARCHAR(255)     NULL,
    rendered_body     TEXT             NULL,
    error_message     VARCHAR(1000)    NULL,
    attempt_count     INT              NOT NULL DEFAULT 0,
    scheduled_at      DATETIME         NULL,
    sent_at           DATETIME         NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_notification_logs_recipient (recipient_user_id),
    INDEX idx_notification_logs_status (status),
    INDEX idx_notification_logs_type_code (type_code),
    INDEX idx_notification_logs_scheduled (status, scheduled_at)
);
```

### Table: `in_app_notifications`

```sql
CREATE TABLE in_app_notifications (
    id                BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    recipient_user_id BIGINT UNSIGNED  NOT NULL,
    type_code         VARCHAR(100)     NOT NULL,
    title             VARCHAR(255)     NOT NULL,
    body              TEXT             NOT NULL,
    is_read           BOOLEAN          NOT NULL DEFAULT FALSE,
    read_at           DATETIME         NULL,
    created_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_in_app_notifications_recipient (recipient_user_id),
    INDEX idx_in_app_notifications_unread (recipient_user_id, is_read)
);
```

### Table: `notification_preferences`

```sql
CREATE TABLE notification_preferences (
    id         BIGINT UNSIGNED  AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT UNSIGNED  NOT NULL,
    type_code  VARCHAR(100)     NOT NULL,
    channel    VARCHAR(20)      NOT NULL,
    is_enabled BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_notification_prefs_user FOREIGN KEY (user_id) REFERENCES users(id),
    UNIQUE uq_notification_preferences (user_id, type_code, channel),
    INDEX idx_notification_preferences_user (user_id)
);
```

---

## 9. Notification Dispatch Flow

```
Caller (any module) → NotificationService.send(NotificationRequest)
    │
    ▼
[NotificationServiceImpl]
    ├── For each requested channel:
    │   ├── Check user preference → SKIP if opted out
    │   ├── Load template for (typeCode, channel, locale)
    │   │   └── FAIL if no template found
    │   ├── Render template with provided variables
    │   ├── If scheduled → persist NotificationLog(status=PENDING, scheduledAt)
    │   └── If immediate:
    │       ├── EMAIL → EmailDispatcher.send(recipient, subject, body)
    │       ├── IN_APP → persist InAppNotification + NotificationLog(status=SENT)
    │       └── PUSH → PushDispatcher.send(userId, title, body)
    └── Each dispatch attempt persists/updates NotificationLog
```

---

## 10. Email Dispatcher Interface

```
interface EmailDispatcher {
    void send(String recipientEmail, String subject, String body): void
}
```

Concrete implementations: `SmtpEmailDispatcher`, `SendGridEmailDispatcher`, etc. The implementation is injected via Spring and selected by configuration. `core-notification` only depends on the interface.

---

## 11. Push Dispatcher Interface

```
interface PushDispatcher {
    void send(Long userId, String title, String body): void
}
```

Push is optional in V1. A `NoOpPushDispatcher` stub is the default implementation until push is configured.

---

## 12. Package Organization

```
com.attendai.core.notification
├── entity
│   ├── NotificationTemplate.java
│   ├── NotificationLog.java
│   ├── InAppNotification.java
│   ├── NotificationPreference.java
│   ├── Channel.java
│   └── NotificationStatus.java
├── repository
│   ├── NotificationTemplateRepository.java
│   ├── NotificationLogRepository.java
│   ├── InAppNotificationRepository.java
│   └── NotificationPreferenceRepository.java
├── service
│   ├── NotificationService.java
│   ├── NotificationServiceImpl.java
│   ├── TemplateRenderer.java
│   ├── EmailDispatcher.java             ← interface
│   └── PushDispatcher.java              ← interface
├── scheduler
│   └── NotificationRetryScheduler.java
├── controller
│   └── NotificationController.java
├── dto
│   ├── SendNotificationRequest.java
│   ├── NotificationTemplateRequest.java
│   ├── NotificationPreferenceRequest.java
│   ├── InAppNotificationResponse.java
│   ├── NotificationLogResponse.java
│   └── NotificationTemplateResponse.java
├── mapper
│   └── NotificationMapper.java
└── exception
    └── NotificationTemplateNotFoundException.java
```

---

## 13. Internal Service API

The primary integration point for all other modules:

```
NotificationService.send(SendNotificationRequest request): void
```

`SendNotificationRequest` contains:
- `recipientUserId`: Long — the user to notify
- `typeCode`: String — notification type code
- `channels`: List<Channel> — which channels to use
- `variables`: Map<String, String> — template variable values
- `scheduledAt`: LocalDateTime (optional, null = send immediately)
- `locale`: String (optional, default `en`)

This is a fire-and-forget call. It never throws to the caller for delivery failures.

---

## 14. System Notification Type Codes (Core-owned)

| Type Code                    | Description                          |
|------------------------------|--------------------------------------|
| `AUTH_PASSWORD_RESET`        | Password reset link                  |
| `AUTH_LOGIN_NEW_DEVICE`      | Login from unrecognized location     |
| `AUTH_TOKEN_REUSE_DETECTED`  | Security alert: token reuse          |
| `SYSTEM_ACCOUNT_CREATED`     | New user account created             |
| `SYSTEM_ACCOUNT_DEACTIVATED` | User account deactivated             |

Business modules register their own type codes (e.g., `SCHOOL_ATTENDANCE_MARKED`, `SCHOOL_LEAVE_APPROVED`). Core does not define them.

---

## 15. API Contracts

Base path: `/api/v1/core/notifications`

### GET /api/v1/core/notifications/inbox — Get In-App Inbox

**Authentication:** Authenticated user (own inbox only)

**Query params:** `page`, `size`, `read` (filter by read status)

**Response 200:** Paginated `InAppNotificationResponse`
```json
{
  "success": true,
  "data": [
    {
      "id": 100,
      "typeCode": "AUTH_PASSWORD_RESET",
      "title": "Password Reset Requested",
      "body": "A password reset was requested for your account.",
      "isRead": false,
      "createdAt": "2025-01-15T10:00:00Z"
    }
  ],
  "pagination": { "page": 0, "size": 20, "totalElements": 3 }
}
```

---

### PATCH /api/v1/core/notifications/inbox/{id}/read — Mark as Read

**Authentication:** Authenticated user (own notifications only)

**Response 200:**
```json
{ "success": true, "data": { "message": "Notification marked as read" } }
```

---

### PATCH /api/v1/core/notifications/inbox/read-all — Mark All as Read

**Authentication:** Authenticated user

**Response 200:**
```json
{ "success": true, "data": { "message": "All notifications marked as read" } }
```

---

### GET /api/v1/core/notifications/preferences — Get Preferences

**Authentication:** Authenticated user (own preferences)

**Response 200:** List of `NotificationPreferenceResponse`

---

### PUT /api/v1/core/notifications/preferences — Update Preference

**Authentication:** Authenticated user

**Request:**
```json
{
  "typeCode": "SCHOOL_ATTENDANCE_MARKED",
  "channel": "EMAIL",
  "isEnabled": false
}
```

**Response 200:**
```json
{ "success": true, "data": { "message": "Preference updated" } }
```

---

### GET /api/v1/core/notifications/templates — List Templates

**Permission:** `CORE_NOTIFICATION_MANAGE`

**Response 200:** Paginated `NotificationTemplateResponse`

---

### POST /api/v1/core/notifications/templates — Create Template

**Permission:** `CORE_NOTIFICATION_MANAGE`

**Request:**
```json
{
  "typeCode": "AUTH_PASSWORD_RESET",
  "channel": "EMAIL",
  "locale": "en",
  "subject": "Reset your AttendAI password",
  "bodyTemplate": "Hello {{firstName}}, click {{resetLink}} to reset your password. This link expires in 1 hour."
}
```

**Response 201:** `NotificationTemplateResponse`

---

### PUT /api/v1/core/notifications/templates/{id} — Update Template

**Permission:** `CORE_NOTIFICATION_MANAGE`

**Response 200:** `NotificationTemplateResponse`

---

### GET /api/v1/core/notifications/logs — Notification Delivery Log

**Permission:** `CORE_NOTIFICATION_MANAGE`

**Query params:** `userId`, `typeCode`, `channel`, `status`, `fromDate`, `toDate`, `page`, `size`

**Response 200:** Paginated `NotificationLogResponse`

---

## 16. Validation Rules

### NotificationTemplateRequest
| Field        | Rule                                             |
|--------------|--------------------------------------------------|
| typeCode     | Not blank, max 100, uppercase + underscores      |
| channel      | Not null, valid `Channel` enum                   |
| locale       | Not blank, max 10, valid locale format           |
| subject      | Required for EMAIL, max 255                      |
| bodyTemplate | Not blank                                        |

### NotificationPreferenceRequest
| Field     | Rule                           |
|-----------|--------------------------------|
| typeCode  | Not blank, max 100             |
| channel   | Not null, valid enum           |
| isEnabled | Not null                       |

---

## 17. Authorization

| Operation                     | Required                             |
|-------------------------------|--------------------------------------|
| Read own inbox                | Any authenticated user               |
| Mark own notification read    | Any authenticated user               |
| Read/update own preferences   | Any authenticated user               |
| View notification templates   | `CORE_NOTIFICATION_MANAGE`           |
| Create/update templates       | `CORE_NOTIFICATION_MANAGE`           |
| View delivery logs            | `CORE_NOTIFICATION_MANAGE`           |

---

## 18. Configuration

| Property                                      | Default | Description                             |
|-----------------------------------------------|---------|-----------------------------------------|
| `attendai.notification.email.enabled`         | `true`  | Enable email channel                    |
| `attendai.notification.push.enabled`          | `false` | Enable push channel                     |
| `attendai.notification.retry.max-attempts`    | `3`     | Max retry attempts for failed dispatch  |
| `attendai.notification.retry.interval-seconds`| `300`   | Retry job interval in seconds           |
| `attendai.notification.email.from`            | (req'd) | Sender email address                    |
| `attendai.notification.email.reply-to`        | (opt)   | Reply-to address                        |

---

## 19. Integration Points

| Module           | Integration                                                         |
|------------------|---------------------------------------------------------------------|
| `core-common`    | `BaseEntity`, exceptions, response types                            |
| `core-user`      | `recipient_user_id` FK; email address resolved from user/person    |
| `core-auth`      | Calls `send()` for password reset and security alert notifications  |
| `core-audit`     | Notification dispatch errors are audit-logged                       |

---

## 20. Error Handling

| Scenario                              | Behaviour                                      |
|---------------------------------------|------------------------------------------------|
| No template for type+channel          | Log as FAILED; do not throw to caller          |
| Missing template variable             | Log as FAILED; do not throw to caller          |
| SMTP/provider unavailable             | Log as FAILED; schedule for retry              |
| Max retries exhausted                 | Status set to PERMANENTLY_FAILED; alert admin  |
| User not found for preference         | `ResourceNotFoundException` (400)              |

---

## 21. Logging and Audit

- Every dispatch attempt persists a `NotificationLog` record.
- Retry exhaustion is logged at WARN level and a `PERMANENTLY_FAILED` log record is persisted.
- Delivered `rendered_body` is stored in `notification_logs` for audit / re-send purposes.

---

## 22. Flyway Migrations

```
V16__create_notification_templates_table.sql
V17__create_notification_logs_table.sql
V18__create_in_app_notifications_table.sql
V19__create_notification_preferences_table.sql
V20__seed_core_notification_templates.sql
```

---

## 23. Testing Strategy

| Test Type        | Scope                                                                   |
|------------------|-------------------------------------------------------------------------|
| Unit — Service   | `send()`: template lookup, rendering, preference check, dispatch        |
| Unit — Service   | Skipped on opt-out, FAILED on missing template                          |
| Unit — Renderer  | `TemplateRenderer`: variable substitution, missing variable behavior    |
| Unit — Scheduler | Retry logic: max attempts, exponential backoff                          |
| Mock dispatchers | `EmailDispatcher` and `PushDispatcher` are mocked in all service tests  |
| Repository test  | Inbox query, unread filter, preference lookup                           |
| Controller test  | Inbox, read, preferences, templates endpoints                           |
| Integration      | `core-auth` triggers password reset → email log persisted               |

---

## 24. Implementation Roadmap

### Task 1: Template entity and repository
- `NotificationTemplate`, `NotificationLog`, `InAppNotification`, `NotificationPreference` entities
- Repositories; Flyway: `V16`–`V19`

### Task 2: Template renderer
- `TemplateRenderer`: regex-based `{{variable}}` substitution
- Handle missing required variables (configurable: fail or empty string)

### Task 3: Dispatcher interfaces and stubs
- `EmailDispatcher` interface + `NoOpEmailDispatcher` stub
- `PushDispatcher` interface + `NoOpPushDispatcher` stub
- `SmtpEmailDispatcher` implementation

### Task 4: Core notification service
- `NotificationServiceImpl.send()`: preference check → template lookup → render → dispatch
- In-app persistence (synchronous)
- `NotificationLog` persistence for all attempts

### Task 5: Retry scheduler
- `NotificationRetryScheduler` (`@Scheduled`): query FAILED logs within max-attempts, retry

### Task 6: Controller and DTOs
- Inbox, read, preferences, templates, logs endpoints

### Task 7: System template seeding
- Flyway: `V20__seed_core_notification_templates.sql`

### Task 8: Audit integration

---

## 25. Acceptance Criteria

- [ ] `NotificationService.send()` never throws to the caller regardless of delivery failure
- [ ] Email is not sent if user has opted out of that notification type on the EMAIL channel
- [ ] In-app notifications are always persisted (even if email delivery fails)
- [ ] A notification with no matching template is logged as FAILED
- [ ] FAILED notifications are retried up to the configured maximum
- [ ] After max retries, status is set to PERMANENTLY_FAILED
- [ ] Inbox returns only the authenticated user's own notifications
- [ ] Template rendering substitutes all provided variables correctly
- [ ] All dispatch attempts are recorded in `notification_logs`

---

## 26. Out of Scope

- SMS notifications
- Notification batching/digest (daily summary emails)
- Email bounce / unsubscribe webhook handling
- Push notification device token management (V1 stub only)
- Multi-language template selection beyond locale lookup
