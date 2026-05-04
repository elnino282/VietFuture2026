package org.example.QuanLyMuaVu.Config;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Mail mail = new Mail();
    private Ai ai = new Ai();
    private Marketplace marketplace = new Marketplace();
    private Sustainability sustainability = new Sustainability();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public Marketplace getMarketplace() {
        return marketplace;
    }

    public void setMarketplace(Marketplace marketplace) {
        this.marketplace = marketplace;
    }

    public Sustainability getSustainability() {
        return sustainability;
    }

    public void setSustainability(Sustainability sustainability) {
        this.sustainability = sustainability;
    }

    public static class Marketplace {
        private Storage storage = new Storage();

        public Storage getStorage() {
            return storage;
        }

        public void setStorage(Storage storage) {
            this.storage = storage;
        }
    }

    public static class Storage {
        private String paymentProofRoot = "storage/marketplace/payment-proofs";

        public String getPaymentProofRoot() {
            return paymentProofRoot;
        }

        public void setPaymentProofRoot(String paymentProofRoot) {
            this.paymentProofRoot = paymentProofRoot;
        }
    }

    public static class Jwt {
        private String secret;
        private long expirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Mail {
        private String from;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }

    public static class Ai {
        private String baseUrl;
        private String apiKey;
        private String model;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }

    public static class Sustainability {
        private String thresholdSource = "config_default_v1";
        private AlertThresholds alerts = new AlertThresholds();
        private ScoreWeights scoreWeights = new ScoreWeights();
        private EstimationDefaults estimation = new EstimationDefaults();
        private LegacyBackfill legacyBackfill = new LegacyBackfill();

        public String getThresholdSource() {
            return thresholdSource;
        }

        public void setThresholdSource(String thresholdSource) {
            this.thresholdSource = thresholdSource;
        }

        public AlertThresholds getAlerts() {
            return alerts;
        }

        public void setAlerts(AlertThresholds alerts) {
            this.alerts = alerts;
        }

        public ScoreWeights getScoreWeights() {
            return scoreWeights;
        }

        public void setScoreWeights(ScoreWeights scoreWeights) {
            this.scoreWeights = scoreWeights;
        }

        public EstimationDefaults getEstimation() {
            return estimation;
        }

        public void setEstimation(EstimationDefaults estimation) {
            this.estimation = estimation;
        }

        public LegacyBackfill getLegacyBackfill() {
            return legacyBackfill;
        }

        public void setLegacyBackfill(LegacyBackfill legacyBackfill) {
            this.legacyBackfill = legacyBackfill;
        }
    }

    public static class LegacyBackfill {
        private boolean enabled = false;
        private boolean dryRun = true;
        private Integer seasonId;
        private LocalDate fromDate;
        private LocalDate toDate;
        private Integer sampleLimit = 50;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isDryRun() {
            return dryRun;
        }

        public void setDryRun(boolean dryRun) {
            this.dryRun = dryRun;
        }

        public Integer getSeasonId() {
            return seasonId;
        }

        public void setSeasonId(Integer seasonId) {
            this.seasonId = seasonId;
        }

        public LocalDate getFromDate() {
            return fromDate;
        }

        public void setFromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
        }

        public LocalDate getToDate() {
            return toDate;
        }

        public void setToDate(LocalDate toDate) {
            this.toDate = toDate;
        }

        public Integer getSampleLimit() {
            return sampleLimit;
        }

        public void setSampleLimit(Integer sampleLimit) {
            this.sampleLimit = sampleLimit;
        }
    }

    public static class AlertThresholds {
        private BigDecimal lowMaxExclusive = BigDecimal.valueOf(40);
        private BigDecimal mediumMaxExclusive = BigDecimal.valueOf(70);
        private BigDecimal mineralHighMin = BigDecimal.valueOf(60);

        public BigDecimal getLowMaxExclusive() {
            return lowMaxExclusive;
        }

        public void setLowMaxExclusive(BigDecimal lowMaxExclusive) {
            this.lowMaxExclusive = lowMaxExclusive;
        }

        public BigDecimal getMediumMaxExclusive() {
            return mediumMaxExclusive;
        }

        public void setMediumMaxExclusive(BigDecimal mediumMaxExclusive) {
            this.mediumMaxExclusive = mediumMaxExclusive;
        }

        public BigDecimal getMineralHighMin() {
            return mineralHighMin;
        }

        public void setMineralHighMin(BigDecimal mineralHighMin) {
            this.mineralHighMin = mineralHighMin;
        }
    }

    public static class ScoreWeights {
        private BigDecimal dependency = new BigDecimal("0.30");
        private BigDecimal efficiency = new BigDecimal("0.25");
        private BigDecimal productivity = new BigDecimal("0.20");
        private BigDecimal risk = new BigDecimal("0.15");
        private BigDecimal confidence = new BigDecimal("0.10");

        public BigDecimal getDependency() {
            return dependency;
        }

        public void setDependency(BigDecimal dependency) {
            this.dependency = dependency;
        }

        public BigDecimal getEfficiency() {
            return efficiency;
        }

        public void setEfficiency(BigDecimal efficiency) {
            this.efficiency = efficiency;
        }

        public BigDecimal getProductivity() {
            return productivity;
        }

        public void setProductivity(BigDecimal productivity) {
            this.productivity = productivity;
        }

        public BigDecimal getRisk() {
            return risk;
        }

        public void setRisk(BigDecimal risk) {
            this.risk = risk;
        }

        public BigDecimal getConfidence() {
            return confidence;
        }

        public void setConfidence(BigDecimal confidence) {
            this.confidence = confidence;
        }
    }

    public static class EstimationDefaults {
        private BigDecimal atmosphericDepositionKgPerHa = BigDecimal.valueOf(8);
        private BigDecimal irrigationWaterKgPerHaPerEvent = new BigDecimal("1.2");
        private BigDecimal legumeFixationKgPerHa = BigDecimal.valueOf(35);
        private BigDecimal mineralFertilizerNRatio = new BigDecimal("0.18");
        private BigDecimal organicFertilizerNRatio = new BigDecimal("0.06");
        private BigDecimal defaultCropNContentKgPerKgYield = new BigDecimal("0.012");
        private BigDecimal strongActionMinConfidence = new BigDecimal("0.60");
        private BigDecimal strongActionMinNue = BigDecimal.valueOf(45);

        public BigDecimal getAtmosphericDepositionKgPerHa() {
            return atmosphericDepositionKgPerHa;
        }

        public void setAtmosphericDepositionKgPerHa(BigDecimal atmosphericDepositionKgPerHa) {
            this.atmosphericDepositionKgPerHa = atmosphericDepositionKgPerHa;
        }

        public BigDecimal getIrrigationWaterKgPerHaPerEvent() {
            return irrigationWaterKgPerHaPerEvent;
        }

        public void setIrrigationWaterKgPerHaPerEvent(BigDecimal irrigationWaterKgPerHaPerEvent) {
            this.irrigationWaterKgPerHaPerEvent = irrigationWaterKgPerHaPerEvent;
        }

        public BigDecimal getLegumeFixationKgPerHa() {
            return legumeFixationKgPerHa;
        }

        public void setLegumeFixationKgPerHa(BigDecimal legumeFixationKgPerHa) {
            this.legumeFixationKgPerHa = legumeFixationKgPerHa;
        }

        public BigDecimal getMineralFertilizerNRatio() {
            return mineralFertilizerNRatio;
        }

        public void setMineralFertilizerNRatio(BigDecimal mineralFertilizerNRatio) {
            this.mineralFertilizerNRatio = mineralFertilizerNRatio;
        }

        public BigDecimal getOrganicFertilizerNRatio() {
            return organicFertilizerNRatio;
        }

        public void setOrganicFertilizerNRatio(BigDecimal organicFertilizerNRatio) {
            this.organicFertilizerNRatio = organicFertilizerNRatio;
        }

        public BigDecimal getDefaultCropNContentKgPerKgYield() {
            return defaultCropNContentKgPerKgYield;
        }

        public void setDefaultCropNContentKgPerKgYield(BigDecimal defaultCropNContentKgPerKgYield) {
            this.defaultCropNContentKgPerKgYield = defaultCropNContentKgPerKgYield;
        }

        public BigDecimal getStrongActionMinConfidence() {
            return strongActionMinConfidence;
        }

        public void setStrongActionMinConfidence(BigDecimal strongActionMinConfidence) {
            this.strongActionMinConfidence = strongActionMinConfidence;
        }

        public BigDecimal getStrongActionMinNue() {
            return strongActionMinNue;
        }

        public void setStrongActionMinNue(BigDecimal strongActionMinNue) {
            this.strongActionMinNue = strongActionMinNue;
        }
    }
}
