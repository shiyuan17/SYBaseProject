package com.company.common.test.filehealth;

record FileHealthViolation(String path, FileHealthRule rule, String message) {
}
