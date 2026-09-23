package com.wikicollection.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class StreamingProvider {

    private Integer providerId;
    private String providerName;
    private String logoUrl;
    private ProviderAccessType type;
    private String deepLinkUrl;
}
