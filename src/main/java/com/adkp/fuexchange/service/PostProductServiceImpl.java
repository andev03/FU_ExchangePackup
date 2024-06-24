package com.adkp.fuexchange.service;

import com.adkp.fuexchange.dto.PostProductDTO;
import com.adkp.fuexchange.mapper.PostProductMapper;
import com.adkp.fuexchange.pojo.*;
import com.adkp.fuexchange.repository.*;
import com.adkp.fuexchange.request.CreatePostProductRequest;
import com.adkp.fuexchange.request.UpdatePostProductRequest;
import com.adkp.fuexchange.response.MetaResponse;
import com.adkp.fuexchange.response.ResponseObject;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostProductServiceImpl implements PostProductService {

    private final PostProductRepository postProductRepository;

    private final PostProductMapper postProductMapper;

    private final ProductRepository productRepository;

    private final PostTypeRepository postTypeRepository;

    private final PostStatusRepository postStatusRepository;

    private final CampusRepository campusRepository;

    private final ReviewRepository reviewRepository;

    @Autowired
    public PostProductServiceImpl(PostProductRepository postProductRepository, PostProductMapper postProductMapper, ProductRepository productRepository, PostTypeRepository postTypeRepository, PostStatusRepository postStatusRepository, CampusRepository campusRepository, ReviewRepository reviewRepository) {
        this.postProductRepository = postProductRepository;
        this.postProductMapper = postProductMapper;
        this.productRepository = productRepository;
        this.postTypeRepository = postTypeRepository;
        this.postStatusRepository = postStatusRepository;
        this.campusRepository = campusRepository;
        this.reviewRepository = reviewRepository;
    }

    @Override
    public ResponseObject<Object> viewMorePostProduct(int current, Integer campusId, Integer postTypeId, String name, Integer categoryId) {
        Pageable currentProduct = PageRequest.of(0, current);

        String campus = Optional.ofNullable(campusId).map(String::valueOf).orElse("");
        String postType = Optional.ofNullable(postTypeId).map(String::valueOf).orElse("");
        String nameProduct = Optional.ofNullable(name).map(String::valueOf).orElse("");
        String category = Optional.ofNullable(categoryId).map(String::valueOf).orElse("");

        List<PostProductDTO> postProductDTO = postProductMapper.toPostProductDTOList(
                postProductRepository.filterPostProduct(currentProduct, campus, postType, nameProduct, category)
        );
        return ResponseObject.builder()
                .status(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .content("Xem thêm thành công!")
                .data(postProductDTO)
                .meta(new MetaResponse(countPostProduct(campusId, postTypeId, name, categoryId, postProductDTO), current))
                .build();
    }

    @Override
    public ResponseObject<Object> getPostProductById(int postProductId) {

        PostProductDTO postProductDTO =
                postProductMapper.toPostProductDTO(postProductRepository.getPostProductByPostId(postProductId));

        Integer totalReview = reviewRepository.countReviewByPostProductId(postProductId);

        Double totalRating = reviewRepository.calcAvgRatingByPostProductId(postProductId);

        if (totalReview != null && totalRating != null) {

            postProductDTO.setTotalReview(totalReview);

            postProductDTO.setTotalRating(totalRating);

        }

        return ResponseObject.builder()
                .status(HttpStatus.OK.value())
                .message(HttpStatus.OK.name())
                .content("Xem thông tin thành công!")
                .data(postProductDTO)
                .build();
    }

    @Override
    @Transactional
    public PostProductDTO updatePostProduct(UpdatePostProductRequest updatePostProductRequest) {

        PostProduct postProduct = postProductRepository.getReferenceById(updatePostProductRequest.getPostProductId());

        Product product = productRepository.getReferenceById(updatePostProductRequest.getProductId());

        PostType postType = postTypeRepository.getReferenceById(updatePostProductRequest.getPostTypeId());

        Campus campus = campusRepository.getReferenceById(updatePostProductRequest.getCampusId());

        postProduct.setProductId(product);

        postProduct.setPostTypeId(postType);

        postProduct.setCampusId(campus);

        postProduct.setQuantity(updatePostProductRequest.getQuantity());

        postProduct.setCreateDate(LocalDateTime.now());

        postProduct.setContent(updatePostProductRequest.getContent());

        return postProductMapper.toPostProductDTO(postProduct);
    }

    @Override
    @Transactional
    public PostProductDTO createPostProduct(CreatePostProductRequest createPostProductRequest) {

        Product product = productRepository.getReferenceById(createPostProductRequest.getProductId());

        PostType postType = postTypeRepository.getReferenceById(createPostProductRequest.getPostTypeId());

        Campus campus = campusRepository.getReferenceById(createPostProductRequest.getCampusId());

        PostStatus postStatus = postStatusRepository.getReferenceById(2);

        return postProductMapper.toPostProductDTO(
                postProductRepository.save(
                        PostProduct.builder()
                                .productId(product)
                                .postTypeId(postType)
                                .campusId(campus)
                                .postStatusId(postStatus)
                                .quantity(createPostProductRequest.getQuantity())
                                .createDate(LocalDateTime.now())
                                .content(createPostProductRequest.getContent())
                                .build()
                )
        );
    }

    public long countPostProduct(Integer campusId, Integer postTypeId, String name, Integer categoryId, List<PostProductDTO> postProductDTOList) {
        if (campusId == null && postTypeId == null && (name == null || name.isEmpty())) {
            return postProductRepository.count();
        }
        return postProductDTOList.size();
    }
}
