package com.project.Fashion.config.mappers;

import com.project.Fashion.dto.ProductCreateDto;
import com.project.Fashion.dto.ProductResponseDto;
import com.project.Fashion.dto.ProductUpdateDto;
import com.project.Fashion.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Maps a Product entity to the detailed response DTO
    @Mapping(source = "seller.id", target = "sellerId")
    @Mapping(expression = "java(product.getSeller().getFirstName() + \" \" + product.getSeller().getLastName())", target = "sellerName")
    @Mapping(source = "seller.email", target = "sellerEmail")
    ProductResponseDto toProductResponseDto(Product product);

    // Maps the creation DTO to a new Product entity
    Product toProductEntity(ProductCreateDto productCreateDto);

    // Maps the update DTO to an existing Product entity (for PATCH)
    // The @MappingTarget annotation tells MapStruct to update the existing entity
    void updateProductFromDto(ProductUpdateDto dto, @MappingTarget Product entity);
}