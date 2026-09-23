package com.wikicollection.infrastructure.adapter.out.persistence;

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
public class StreamingProviderEntity {

    private Integer providerId;
    private String providerName;
    private String logoUrl;
    private String type;
    private String deepLinkUrl;
}
