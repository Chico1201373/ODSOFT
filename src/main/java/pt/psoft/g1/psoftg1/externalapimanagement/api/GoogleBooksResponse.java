package pt.psoft.g1.psoftg1.externalapimanagement.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleBooksResponse {
    private List<Item> items;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Item {
        private VolumeInfo volumeInfo;

        public VolumeInfo getVolumeInfo() {
            return volumeInfo;
        }

        public void setVolumeInfo(VolumeInfo volumeInfo) {
            this.volumeInfo = volumeInfo;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VolumeInfo {
        private List<IndustryIdentifier> industryIdentifiers;

        public List<IndustryIdentifier> getIndustryIdentifiers() {
            return industryIdentifiers;
        }

        public void setIndustryIdentifiers(List<IndustryIdentifier> industryIdentifiers) {
            this.industryIdentifiers = industryIdentifiers;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IndustryIdentifier {
        private String type;
        private String identifier;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }
    }
}
