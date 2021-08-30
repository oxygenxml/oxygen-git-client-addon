package com.oxygenxml.git.service;

public enum ApplyStashStatus {
  SUCCESSFULLY,
  APPLIED_WITH_GENERATED_CONFLICTS,
  UNCOMMITTED_FILES, 
  BUG_CONFLICT,
  CONFLICTS,
  UNKNOWN_CAUSE
}
