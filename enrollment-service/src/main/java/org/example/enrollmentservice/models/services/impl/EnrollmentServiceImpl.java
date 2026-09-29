package org.example.enrollmentservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.enrollmentservice.models.constants.EnrollmentStatus;
import org.example.enrollmentservice.exceptions.DuplicateCourseException;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentDetailRequest;
import org.example.enrollmentservice.models.dto.requests.CreateEnrollmentRequest;
import org.example.enrollmentservice.models.dto.responses.EnrollmentDetailResponse;
import org.example.enrollmentservice.models.dto.responses.EnrollmentResponse;
import org.example.enrollmentservice.models.dto.responses.CourseResponse;
import org.example.enrollmentservice.models.entities.Enrollment;
import org.example.enrollmentservice.models.entities.EnrollmentDetail;
import org.example.enrollmentservice.models.repositories.EnrollmentDetailRepository;
import org.example.enrollmentservice.models.repositories.EnrollmentRepository;
import org.example.enrollmentservice.models.services.EnrollmentService;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

        private final EnrollmentRepository enrollmentRepository;
        private final EnrollmentDetailRepository enrollmentDetailRepository;
        private final CourseGatewayService courseGatewayService;

        @Override
        @Transactional
        public EnrollmentResponse createEnrollment(CreateEnrollmentRequest request) {
                List<EnrollmentDetail> enrollmentDetails = new ArrayList<>();
                BigDecimal totalPrice = BigDecimal.ZERO;
                for (CreateEnrollmentDetailRequest detailRequest : request.items()) {
                        CourseResponse courseResponse = courseGatewayService.getCourseById(detailRequest.courseId());
                        if (enrollmentDetails.stream().anyMatch(detail -> detail.getCourseId().equals(courseResponse.courseId()))) {
                                throw new DuplicateCourseException(courseResponse.courseName());
                        }
                        EnrollmentDetail enrollmentDetail = new EnrollmentDetail();
                        enrollmentDetail.setCourseId(courseResponse.courseId());
                        enrollmentDetail.setCourseName(courseResponse.courseName());
                        enrollmentDetail.setCourseFee(courseResponse.courseFee());
                        enrollmentDetails.add(enrollmentDetail);
                        totalPrice = totalPrice.add(courseResponse.courseFee());
                }
                OrderResponse orderResponse = createOrder(request.studentEmail(), totalPrice);
                Enrollment enrollment = new Enrollment();
                enrollment.setUserId(request.studentEmail());
                enrollment.setTotalPrice(totalPrice);
                enrollment.setStatus(EnrollmentStatus.PENDING);
                enrollment.setOrderId(orderResponse.getOrderId());
                enrollment.setCreatedAt(LocalDateTime.now());
                enrollmentRepository.save(enrollment);
                for (EnrollmentDetail enrollmentDetail : enrollmentDetails) {
                        enrollmentDetail.setEnrollment(enrollment);
                        enrollmentDetailRepository.save(enrollmentDetail);
                }
                EnrollmentResponse enrollmentResponse = new EnrollmentResponse();
                enrollmentResponse.setEnrollmentId(enrollment.getId());
                enrollmentResponse.setUserId(enrollment.getUserId());
                enrollmentResponse.setTotalPrice(enrollment.getTotalPrice());
                enrollmentResponse.setStatus(enrollment.getStatus());
                List<EnrollmentDetailResponse> enrollmentDetailResponses = new ArrayList<>();
                for (EnrollmentDetail enrollmentDetail : enrollmentDetails) {
                        EnrollmentDetailResponse enrollmentDetailResponse = new EnrollmentDetailResponse();
                        enrollmentDetailResponse.setCourseId(enrollmentDetail.getCourseId());
                        enrollmentDetailResponse.setCourseName(enrollmentDetail.getCourseName());
                        enrollmentDetailResponse.setPrice(enrollmentDetail.getPrice());
                        enrollmentDetailResponses.add(enrollmentDetailResponse);
                }
                enrollmentResponse.setEnrollmentDetails(enrollmentDetailResponses);
                return enrollmentResponse;
                throw new UnsupportedOperationException();

        }
}
