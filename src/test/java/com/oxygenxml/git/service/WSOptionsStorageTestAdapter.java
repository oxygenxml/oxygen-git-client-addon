package com.oxygenxml.git.service;

import java.util.LinkedHashMap;
import java.util.Map;

import ro.sync.exml.workspace.api.options.ExternalPersistentObject;
import ro.sync.exml.workspace.api.options.WSOptionListener;
import ro.sync.exml.workspace.api.options.WSOptionsStorage;

/**
 * A test storage that keep options in a map.
 * 
 * @author alex_jitianu
 */
public class WSOptionsStorageTestAdapter implements WSOptionsStorage {
  /**
   * The persisted map;
   */
  private Map<String, Object> saved = new LinkedHashMap<>();
  @Override
  public void setOptionsDoctypePrefix(String optionsDoctypePrefix) {
    // Recommended not to be used.
  }

  @Override
  public void addOptionListener(WSOptionListener listener) {
  }

  @Override
  public void removeOptionListener(WSOptionListener listener) {
  }

  @Override
  public String getOption(String key, String defaultValue) {
    return (String) saved.getOrDefault(key, defaultValue);
  }

  @Override
  public void setOption(String key, String value) {
    saved.put(key, value);
  }

  @Override
  public void setPersistentObjectOption(String key, ExternalPersistentObject persistentObject) {
    saved.put(key, persistentObject.clone());
  }

  @Override
  public ExternalPersistentObject getPersistentObjectOption(String key, ExternalPersistentObject defaultValue) {
    return (ExternalPersistentObject) ((ExternalPersistentObject) saved.getOrDefault(key, defaultValue)).clone();
  }

  @Override
  public String[] getStringArrayOption(String key, String[] defaultValues) {
    return (String[]) saved.getOrDefault(key, defaultValues);
  }

  @Override
  public void setStringArrayOption(String key, String[] values) {
    saved.put(key, values.clone());
  }
}
