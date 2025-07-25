package com.enrollment.e2e.config;

/**
 * Defines test execution profiles for E2E tests.
 * Allows switching between mock and real service testing.
 */
public enum E2ETestProfile {
    /**
     * Uses WireMock for all services - fast, predictable, no external dependencies
     */
    MOCK("mock", "Mock all services with WireMock"),
    
    /**
     * Uses real services in containers - slower, comprehensive, requires Docker
     */
    INTEGRATION("integration", "Use real services in containers"),
    
    /**
     * Hybrid approach - infrastructure in containers, services mocked
     */
    HYBRID("hybrid", "MongoDB/Redis in containers, services mocked"),
    
    /**
     * Manual mode - expects services to be already running
     */
    MANUAL("manual", "Services must be manually started");
    
    private final String profileName;
    private final String description;
    
    E2ETestProfile(String profileName, String description) {
        this.profileName = profileName;
        this.description = description;
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the active profile from system property or environment variable
     */
    public static E2ETestProfile getActiveProfile() {
        String profileStr = System.getProperty("e2e.test.profile", 
            System.getenv().getOrDefault("E2E_TEST_PROFILE", "hybrid"));
        
        for (E2ETestProfile profile : values()) {
            if (profile.profileName.equalsIgnoreCase(profileStr)) {
                return profile;
            }
        }
        
        System.out.println("Unknown profile: " + profileStr + ", defaulting to HYBRID");
        return HYBRID;
    }
    
    /**
     * Check if mocks should be used based on profile
     */
    public boolean shouldUseMocks() {
        return this == MOCK || this == HYBRID;
    }
    
    /**
     * Check if real services should be used
     */
    public boolean shouldUseRealServices() {
        return this == INTEGRATION || this == MANUAL;
    }
    
    /**
     * Check if containers should be used for infrastructure
     */
    public boolean shouldUseContainers() {
        return this != MANUAL;
    }
}