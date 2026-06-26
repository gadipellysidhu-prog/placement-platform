CLAUDE.md
Document Title: CLAUDE.md — Operational Handbook for Frontend Development
Purpose: This document defines Claude's operational procedures, responsibilities, and workflows for the frontend development lifecycle. It serves as the authoritative operational reference for all implementation activities.
Audience: Claude AI Assistant, Engineering Team, Technical Leads, and Project Stakeholders
Status: Active
Version: 1.0.0
Last Updated: 2026-06-26
Owner: Principal Software Architecture Team

Table of Contents
Scope

Responsibilities

Repository Inspection

Phase 0: Planning and Discovery

Development Workflow

Phase Deliverables

Backend Integration Workflow

Documentation Workflow

Git Workflow

Quality Gates

Reports

Review Process

Operational Rules

References

1. Scope
This document defines operational procedures for frontend development activities. It does not define engineering philosophy, architecture principles, coding standards, or design patterns. Those governance items are codified in FRONTEND_CONSTITUTION.md, which this document references where applicable. All operational procedures MUST align with the principles defined in the constitution.

2. Responsibilities
Claude SHALL assume the following operational responsibilities across the frontend development lifecycle:

2.1 Repository Inspection Responsibility
Claude MUST perform comprehensive repository inspection before any implementation activity. The inspection SHALL establish the current state of the codebase and inform all subsequent planning and development.

2.2 Planning Responsibility
Claude MUST generate all planning documents for each phase before proceeding to implementation. Planning documents SHALL be complete, reviewed, and approved before any code is written.

2.3 Documentation Responsibility
Claude MUST update all affected documentation whenever implementation changes occur. Documentation SHALL remain synchronized with the implementation at all times.

2.4 Development Responsibility
Claude MUST implement features according to the approved phase plan. Implementation SHALL follow the development workflow and adhere to quality gates.

2.5 Backend Integration Responsibility
Claude MUST verify all backend integration points against the actual repository. Integration SHALL use only existing, verified APIs and DTOs.

2.6 Review Responsibility
Claude MUST participate in review processes for all phases. Reviews SHALL be thorough and documented.

2.7 Validation Responsibility
Claude MUST validate all deliverables against quality gates. Validation SHALL be comprehensive and documented.

2.8 Reporting Responsibility
Claude MUST generate and maintain all required reports. Reports SHALL be updated after every phase.

2.9 Git Responsibility
Claude MUST manage Git commits according to the defined workflow. Commits SHALL be atomic, descriptive, and pass all quality gates.

2.10 Phase Completion Responsibility
Claude MUST complete each phase in its entirety before proceeding. Phase completion SHALL be documented and approved.

3. Repository Inspection
3.1 Mandatory Inspection Requirements
Before any implementation activity, Claude MUST inspect the repository to establish verified facts. The inspection SHALL include, but is not limited to:

3.1.1 Repository Structure

Directory organization

Module structure

Package conventions

Build configuration

3.1.2 Existing Frontend

Current user interface implementation

Component hierarchy

State management patterns

Styling approach

Routing configuration

3.1.3 Backend Modules

Module names and responsibilities

Package structure

Service boundaries

API versioning

3.1.4 REST Controllers

Controller endpoints

HTTP methods

Request mappings

Response structures

Error handling patterns

3.1.5 Request Mappings

URL patterns

Path variables

Query parameters

Request body requirements

3.1.6 DTOs

Data transfer object definitions

Field names and types

Validation annotations

Serialization patterns

3.1.7 Validation

Validation rules

Validation groups

Custom validators

Error messages

3.1.8 Authentication

Authentication mechanism

Token handling

Session management

Security headers

3.1.9 Authorization

Permission models

Role definitions

Access controls

Security expressions

3.1.10 Roles

Defined roles

Role hierarchies

Permissions per role

Role-based access control

3.1.11 Database Schema

Table structures

Column definitions

Relationships

Indices

Constraints

3.1.12 Flyway Migrations

Migration scripts

Version history

Schema evolution

3.1.13 Environment Variables

Required variables

Default values

Environment-specific configuration

Secrets management

3.1.14 Docker

Dockerfile configuration

Docker Compose setup

Container orchestration

Multi-stage builds

3.1.15 CI/CD

Pipeline definitions

Build stages

Deployment environments

Automated testing

3.1.16 Existing Documentation

README files

API documentation

Setup guides

Development guides

3.1.17 Existing Assets

Images

Icons

Fonts

Static resources

3.1.18 Build Tooling

Bundler configuration

Build scripts

Development server

Production builds

3.1.19 Quality Tooling

Linter configuration

Formatter configuration

Testing framework

Coverage reporting

3.2 Source of Truth
The repository SHALL be the implementation source of truth. Claude MUST NOT make assumptions about repository contents. All observations SHALL be derived from actual repository inspection.

3.3 Fact Classification
Claude MUST classify inspection findings into three categories:

3.3.1 Verified Facts

Directly observed in repository

Confirmed by multiple sources

Documented in existing code

3.3.2 Inferences

Derived from verified facts

Based on patterns and conventions

Reasonable assumptions

3.3.3 Recommendations

Based on best practices

Suggested improvements

Future considerations

4. Phase 0: Planning and Discovery
4.1 Phase 0 Requirements
Before any implementation, Claude MUST generate every required planning document. Planning SHALL be comprehensive and SHALL establish the foundation for all subsequent phases.

4.2 Required Planning Documents
Claude MUST generate the following planning documents:

4.2.1 Project Charter

Project vision and objectives

Stakeholder identification

Success criteria

Constraints and assumptions

4.2.2 Technical Specification

Technology stack selection

Infrastructure requirements

Integration points

Performance requirements

4.2.3 Feature Breakdown

Feature list

Feature priorities

Feature dependencies

Effort estimation

4.2.4 Phase Plan

Phase definitions

Phase deliverables

Phase timelines

Phase dependencies

4.2.5 Architecture Overview

High-level architecture

Component diagrams

Data flow

Integration architecture

4.2.6 API Integration Plan

API endpoints to consume

DTO definitions

Authentication integration

Error handling strategy

4.2.7 Data Model

Entity definitions

Relationships

State structure

Data persistence strategy

4.2.8 Component Design

Component hierarchy

Component responsibilities

Component interactions

State management design

4.2.9 Routing Design

Route definitions

Navigation structure

Route guards

Lazy loading strategy

4.2.10 Styling Strategy

Design system adoption

Theme configuration

Responsive approach

Accessibility standards

4.2.11 Testing Strategy

Unit test coverage targets

Integration test approach

End-to-end test plan

Test environment setup

4.2.12 Quality Standards

Code quality metrics

Performance targets

Accessibility requirements

Security standards

4.2.13 Risk Assessment

Technical risks

Integration risks

Resource risks

Timeline risks

4.2.14 Deployment Plan

Environment strategy

Deployment process

Rollback procedures

Monitoring strategy

4.3 Phase 0 Approval
Phase 0 SHALL NOT be considered complete until all planning documents have been reviewed and approved. Claude MUST NOT proceed to implementation without explicit approval.

5. Development Workflow
5.1 Workflow Definition
Claude SHALL follow a sequential, phase-based development workflow. Each phase MUST be completed before proceeding to the next.

5.2 Workflow Sequence
The development workflow SHALL follow this sequence:

text
Phase 0 (Planning)
        ↓
     Review
        ↓
     Commit
        ↓
Phase 1 (Implementation)
        ↓
     Review
        ↓
     Commit
        ↓
Phase 2 (Implementation)
        ↓
     Review
        ↓
     Commit
        ↓
    Continue until production readiness
5.3 Workflow Rules
5.3.1 No Skipping Phases

Each phase MUST be executed in sequence

Phases SHALL NOT be combined

Phase dependencies MUST be respected

5.3.2 No Skipping Reviews

Each phase SHALL undergo review

Reviews SHALL be documented

Review feedback SHALL be addressed

5.3.3 No Skipping Quality Gates

Quality gates SHALL be applied to every phase

Failed gates MUST be resolved

Gates SHALL NOT be bypassed

6. Phase Deliverables
6.1 Deliverable Requirements
Each phase MUST produce all required deliverables. Deliverables SHALL be complete and of production quality.

6.2 Required Deliverables
Each phase SHALL produce:

6.2.1 Completed Features

Features as defined in the phase plan

Fully functional implementation

User-facing functionality

Backend integration

6.2.2 Documentation Updates

Updated technical documentation

Updated user guides

Updated API documentation

Updated setup guides

6.2.3 Files Created

New source files

New configuration files

New test files

New asset files

6.2.4 Files Modified

Modified source files

Modified configuration files

Modified test files

Modified asset files

6.2.5 API Integrations

API calls implemented

Response handling

Error handling

Data transformation

6.2.6 Tests

Unit tests

Integration tests

Component tests

E2E tests (if applicable)

6.2.7 Screenshots

UI screenshots for visual changes

Before and after comparison

Responsive view screenshots

6.2.8 Known Limitations

Identified limitations

Technical debt incurred

Known issues

Future improvements

6.2.9 Quality Gate Report

Quality gate results

Metrics and measurements

Pass/fail status

Remediation actions

6.2.10 Project Health Report

Health metrics

Trend analysis

Risk assessment

Recommendations

6.2.11 Git Commit

Clean commit history

Descriptive commit message

Complete change set

7. Backend Integration Workflow
7.1 Integration Requirements
Claude MUST verify all backend integration points against the actual repository. Integration SHALL be based on verified facts only.

7.2 Integration Steps
7.2.1 Repository Inspection

Inspect backend code

Identify available endpoints

Document API contracts

7.2.2 API Verification

Verify each endpoint exists

Verify HTTP methods

Verify request/response formats

Verify error responses

7.2.3 DTO Verification

Verify DTO field names

Verify DTO field types

Verify DTO validation annotations

Verify DTO serialization

7.2.4 Authentication Verification

Verify authentication mechanism

Verify token requirements

Verify token refresh

Verify session handling

7.2.5 Role Verification

Verify available roles

Verify role permissions

Verify role-based access

Verify role hierarchy

7.2.6 Validation Verification

Verify validation rules

Verify validation groups

Verify custom validators

Verify error handling

7.3 Integration Rules
7.3.1 Never Invent APIs

APIs MUST be discovered, not assumed

Invented APIs are unacceptable

Missing APIs MUST be documented

7.3.2 Never Invent DTOs

DTOs MUST be based on actual definitions

Invented DTOs are unacceptable

Missing DTOs MUST be documented

7.3.3 Never Invent Backend Functionality

All backend interaction SHALL be based on actual functionality

Assumed functionality is unacceptable

Missing functionality MUST be documented

7.3.4 Document Missing Functionality

Missing APIs SHALL be documented

Missing DTOs SHALL be documented

Missing functionality SHALL be documented

Documentation SHALL include recommendations

8. Documentation Workflow
8.1 Documentation Requirements
Claude MUST maintain comprehensive documentation throughout the development lifecycle. Documentation SHALL be updated whenever implementation changes occur.

8.2 Documentation Rules
8.2.1 Update All Affected Documents

Every implementation change SHALL trigger documentation updates

All affected documents SHALL be identified

Updates SHALL be complete

Outdated documentation is unacceptable

8.2.2 Cross-Reference Documents

Documents SHALL reference related documents

Cross-references SHALL be accurate

Navigation SHALL be clear

8.2.3 Avoid Duplication

Information SHALL be captured once

Duplicate content SHALL be eliminated

References SHALL be used instead

8.2.4 Maintain Synchronization

Documentation SHALL reflect implementation

Implementation SHALL reflect documentation

Inconsistencies SHALL be resolved

9. Git Workflow
9.1 Commit Requirements
Claude MUST follow a disciplined Git workflow. Commits SHALL be clean, descriptive, and atomic.

9.2 Commit Rules
9.2.1 One Commit Per Phase

Each phase SHALL result in one commit

Multiple commits within a phase are discouraged

Squash commits when appropriate

9.2.2 Commit After Quality Gates

Commits SHALL occur only after all quality gates pass

Failed gates SHALL be resolved before commit

Committing broken code is unacceptable

9.2.3 Descriptive Commit Messages

Commit messages SHALL be descriptive

Messages SHALL explain what and why

Messages SHALL follow conventional format

9.2.4 Never Commit Failing Code

Failing code SHALL NOT be committed

All tests SHALL pass

All builds SHALL succeed

9.2.5 Never Commit Incomplete Phases

Incomplete phases SHALL NOT be committed

Phase deliverables SHALL be complete

Phase approval SHALL be obtained

10. Quality Gates
10.1 Quality Gate Requirements
Claude MUST apply quality gates to every phase. Quality gates SHALL ensure production-ready deliverables.

10.2 Mandatory Quality Gates
10.2.1 Build Quality Gate

npm run build SHALL succeed

Production builds SHALL be error-free

Bundle size SHALL be within limits

10.2.2 Lint Quality Gate

npm run lint SHALL pass

No linting errors SHALL exist

Linting warnings SHALL be reviewed

10.2.3 Type Quality Gate

npm run typecheck SHALL pass

No TypeScript errors SHALL exist

Strict type checking SHALL be enabled

10.2.4 Test Quality Gate

npm run test SHALL pass

All tests SHALL succeed

Coverage targets SHALL be met

10.2.5 Accessibility Quality Gate

Accessibility reviews SHALL be performed

WCAG standards SHALL be met

Accessibility issues SHALL be resolved

10.2.6 Responsive Quality Gate

Responsive verification SHALL be performed

All viewport sizes SHALL be supported

Responsive issues SHALL be resolved

10.2.7 Backend Integration Quality Gate

Backend integration verification SHALL be performed

All integrations SHALL work correctly

Integration issues SHALL be resolved

10.2.8 Documentation Quality Gate

Documentation updates SHALL be complete

Documentation SHALL be accurate

Documentation issues SHALL be resolved

10.2.9 Project Health Quality Gate

Project Health SHALL be updated

Health metrics SHALL be reviewed

Health issues SHALL be resolved

10.2.10 Git Quality Gate

Git commit SHALL be complete

Commit message SHALL be descriptive

Commit issues SHALL be resolved

10.3 Gate Failure Resolution
Gate failures SHALL be resolved before proceeding

Resolution SHALL be documented

Gates SHALL NOT be bypassed

Exceptions SHALL require approval

11. Reports
11.1 Report Requirements
Claude MUST generate and maintain all required reports. Reports SHALL be updated after every phase.

11.2 Required Reports
11.2.1 Project Health Report (PROJECT_HEALTH.md)

Overall project health status

Code quality metrics

Test coverage metrics

Technical debt assessment

Risk assessment

Recommendations

Trend analysis

11.2.2 Phase Report (PHASE_REPORT.md)

Phase identification

Phase deliverables

Completion status

Issues encountered

Resolutions applied

Lessons learned

Next steps

11.2.3 Quality Gate Report (QUALITY_GATE_REPORT.md)

Quality gate results

Pass/fail status per gate

Metrics and measurements

Remediation actions

Review findings

Approval status

11.3 Report Updates
Reports SHALL be updated after every phase

Reports SHALL be accurate and complete

Reports SHALL be reviewed and approved

Report format SHALL be consistent

12. Review Process
12.1 Review Requirements
Claude MUST participate in comprehensive review processes for all phases. Reviews SHALL ensure quality and compliance.

12.2 Required Reviews
12.2.1 Self-Review

Claude SHALL review its own work

Work SHALL be checked against requirements

Issues SHALL be self-identified and resolved

Self-review SHALL be documented

12.2.2 Architecture Review

Architecture SHALL be reviewed

Design decisions SHALL be validated

Patterns SHALL be verified

Technical debt SHALL be assessed

12.2.3 Code Review

Code SHALL be peer-reviewed

Code quality SHALL be assessed

Standards compliance SHALL be verified

Code issues SHALL be resolved

12.2.4 Documentation Review

Documentation SHALL be reviewed

Accuracy SHALL be verified

Completeness SHALL be assessed

Documentation issues SHALL be resolved

12.2.5 Backend Integration Review

API integration SHALL be reviewed

DTO usage SHALL be verified

Authentication SHALL be tested

Integration issues SHALL be resolved

12.2.6 Quality Review

Quality standards SHALL be reviewed

Metrics SHALL be assessed

Compliance SHALL be verified

Quality issues SHALL be resolved

12.3 Review Completion
Reviews SHALL be completed for every phase

Review findings SHALL be addressed

Approval SHALL be obtained

Phase SHALL NOT proceed without review

13. Operational Rules
13.1 Mandatory Rules
The following rules MUST be followed at all times:

13.1.1 Never Skip Repository Inspection

Inspection SHALL be performed before implementation

Inspection SHALL be comprehensive

Inspection findings SHALL be documented

13.1.2 Never Skip Documentation

Documentation SHALL be maintained

Documentation SHALL be updated

Documentation SHALL be accurate

13.1.3 Never Skip Testing

Tests SHALL be written

Tests SHALL pass

Tests SHALL achieve coverage targets

13.1.4 Never Skip Backend Verification

Backend integration SHALL be verified

Verified facts SHALL be used

Missing functionality SHALL be documented

13.1.5 Never Implement Outside Approved Phase

Only approved phase work SHALL be implemented

Scope creep SHALL be prevented

Changes SHALL be approved

13.1.6 Never Proceed With Failing Quality Gates

Quality gates SHALL pass

Failures SHALL be resolved

Resolution SHALL be documented

13.1.7 Never Modify Architecture Without Documentation

Architecture changes SHALL be documented

Impact SHALL be assessed

Approval SHALL be obtained

13.1.8 Never Ignore Repository Conflicts

Conflicts SHALL be resolved

Resolution SHALL be documented

Collaboration SHALL be maintained

13.1.9 Never Continue After Phase 0 Without Approval

Phase 0 SHALL be complete

Approval SHALL be obtained

Planning SHALL be documented

14. References
14.1 Referenced Documents
The following documents are referenced in this operational handbook. Their contents are not duplicated here.

14.1.1 Governance Documents

FRONTEND_CONSTITUTION.md — Defines engineering philosophy, architecture principles, coding standards, and design patterns

14.1.2 Master Documents

MASTER_PROMPT.md — Defines overall project context and master planning

14.1.3 Planning Documents

Project Charter

Technical Specification

Feature Breakdown

Phase Plan

Architecture Overview

API Integration Plan

Data Model

Component Design

Routing Design

Styling Strategy

Testing Strategy

Quality Standards

Risk Assessment

Deployment Plan

14.1.4 Architecture Documents

System Architecture

Component Architecture

Data Architecture

Integration Architecture

Security Architecture

14.1.5 Governance Documents

Coding Standards

Documentation Standards

Testing Standards

Quality Standards

Security Standards

14.1.6 Quality Documents

Project Health Report

Phase Report

Quality Gate Report

14.2 Document Management
All referenced documents SHALL be maintained in the repository

References SHALL be accurate and up-to-date

Cross-references SHALL be validated during reviews

Missing or outdated documents SHALL be updated

End of Document