package de.iani.headcommands.model;

public record ApiMeta(String apiVersion, boolean demoMode, boolean dataLimited, String license, int records) {
    public boolean hasFreeFields() {
        return "free".equalsIgnoreCase(license) || "bronze".equalsIgnoreCase(license) || "silver".equalsIgnoreCase(license) || "gold".equalsIgnoreCase(license);
    }
}
