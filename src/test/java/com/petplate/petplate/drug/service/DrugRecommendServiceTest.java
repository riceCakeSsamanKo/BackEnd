package com.petplate.petplate.drug.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.petplate.petplate.common.EmbeddedType.StandardNutrient;
import com.petplate.petplate.dailyMealNutrient.service.DeficientNutrientService;
import com.petplate.petplate.drug.domain.entity.Drug;
import com.petplate.petplate.drug.domain.entity.DrugNutrient;
import com.petplate.petplate.drug.dto.request.DrugFindRequestDto;
import com.petplate.petplate.drug.dto.response.DrugResponseDto;
import com.petplate.petplate.drug.dto.response.RecommendDrugResponseDto;
import com.petplate.petplate.drug.dto.response.RecommendDrugResponseDtoWithNutrientName;
import com.petplate.petplate.drug.repository.DrugNutrientRepository;
import com.petplate.petplate.drug.repository.DrugRepository;
import com.petplate.petplate.pet.dto.response.ReadPetNutrientResponseDto;
import com.petplate.petplate.pet.service.PetService;
import java.time.LocalDate;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class DrugRecommendServiceTest {

    @InjectMocks
    DrugRecommendService drugRecommendService;

    @Mock
    DrugRepository drugRepository;

    @Mock
    DrugNutrientRepository drugNutrientRepository;

    @Mock
    DeficientNutrientService deficientNutrientService;


    private Drug getTestDrug(){

        Drug drug = Drug.builder()
                .drugImgPath("img.path")
                .name("테스트drug")
                .englishName("testDrug")
                .vendor("naver")
                .url("www.naver.com")
                .build();

        ReflectionTestUtils.setField(drug,"id",1L);

        return drug;
    }

    private List<DrugNutrient> getTestDrugNutrientList(Drug drug){

        DrugNutrient drugNutrient1 =DrugNutrient.builder()
                .drug(drug)
                .standardNutrient(StandardNutrient.PROTEIN)
                .build();

        ReflectionTestUtils.setField(drugNutrient1,"id",2L);

        DrugNutrient drugNutrient2 =DrugNutrient.builder()
                .drug(drug)
                .standardNutrient(StandardNutrient.CARBON_HYDRATE)
                .build();

        ReflectionTestUtils.setField(drugNutrient2,"id",3L);

        DrugNutrient drugNutrient3 =DrugNutrient.builder()
                .drug(drug)
                .standardNutrient(StandardNutrient.PHOSPHORUS)
                .build();

        ReflectionTestUtils.setField(drugNutrient3,"id",4L);

        return List.of(drugNutrient1,drugNutrient2,drugNutrient3);

    }


    @Test
    @DisplayName("특정 영양소 하나를 포함하는 영양제 조회")
    public void 영양소_하나로_영양제_조회(){
        //given
        Drug drug = getTestDrug();
        List<DrugNutrient> drugNutrientList = getTestDrugNutrientList(drug);
        given(drugNutrientRepository.findByStandardNutrientWithFetchDrug(any(StandardNutrient.class))).willReturn(List.of(drugNutrientList.get(1)));



        //when
        List<RecommendDrugResponseDto> drugResponseDtoList = drugRecommendService.findDrugByNutrientName("탄수화물");


        //then
        assertThat(drugResponseDtoList.size()).isEqualTo(1);

    }

    @Test
    @DisplayName("특정 영양소를 가장많이 포함하는 영양제 순서대로 조회")
    public void 영양소_여러개로_영양제_조회(){
        //given
        Drug drug = getTestDrug();

        List<DrugNutrient> drugNutrientList = getTestDrugNutrientList(drug);

        given(drugRepository.findUserProperDrugList(eq(List.of(StandardNutrient.CARBON_HYDRATE)))
        ).willReturn(List.of(drug));

        given(drugRepository.findUserProperDrugList(eq(List.of(StandardNutrient.PROTEIN)))
        ).willReturn(List.of(drug));




        //when
        List<RecommendDrugResponseDtoWithNutrientName> drugResponseDtoList = drugRecommendService.findDrugByVariousNutrientName(
                DrugFindRequestDto.builder()
                        .nutrients(List.of("탄수화물","단백질"))
                        .build());




        //then
        assertThat(drugResponseDtoList.size()).isEqualTo(2);

    }

    @Test
    @DisplayName("Pet의 부족 영양분 기반 추천 영양제")
    public void Pet_부족_영양분_기반_추천_영양제(){
        //given
        Drug drug = getTestDrug();

        List<DrugNutrient> drugNutrientList = getTestDrugNutrientList(drug);

        given(deficientNutrientService.getDeficientNutrients(anyString(),anyLong(),eq(1L))).willReturn(
                List.of(ReadPetNutrientResponseDto.of("탄수화물",null,null,3.5,2.6,1.3),
                        ReadPetNutrientResponseDto.of("지방",null,null,3.6,2.6,1.3))
        );
        given(deficientNutrientService.getDeficientNutrients(anyString(),anyLong(),eq(2L))).willReturn(
                List.of(ReadPetNutrientResponseDto.of("비타민 A",null,null,3.5,2.6,1.3)
                      )
        );

        given(drugRepository.findUserProperDrugList(eq(List.of(StandardNutrient.CARBON_HYDRATE)))
        ).willReturn(List.of(drug));

        given(drugRepository.findUserProperDrugList(eq(List.of(StandardNutrient.FAT)))
        ).willReturn(List.of(drug));

        given(drugRepository.findUserProperDrugList(eq(List.of(StandardNutrient.VITAMIN_A)))
        ).willReturn(List.of());



        //when
        List<RecommendDrugResponseDtoWithNutrientName> recommendDrugResponseDtoWithNutrientNames1 = drugRecommendService.findDrugByDeficientNutrientsName("any",1L,1L);
        List<RecommendDrugResponseDtoWithNutrientName> recommendDrugResponseDtoWithNutrientNames2 = drugRecommendService.findDrugByDeficientNutrientsName("any",1L,2L);




        //then
        assertThat(recommendDrugResponseDtoWithNutrientNames1.size()).isEqualTo(2);
        assertThat(recommendDrugResponseDtoWithNutrientNames2.size()).isEqualTo(1);
        assertThat(recommendDrugResponseDtoWithNutrientNames1.get(0).getNutrientName()).isEqualTo("탄수화물");
        assertThat(recommendDrugResponseDtoWithNutrientNames1.get(0).getDrugResponseDtoList().size()).isEqualTo(1);
        assertThat(recommendDrugResponseDtoWithNutrientNames1.get(1).getNutrientName()).isEqualTo("지방");
        assertThat(recommendDrugResponseDtoWithNutrientNames2.get(0).getNutrientName()).isEqualTo("비타민 A");
        assertThat(recommendDrugResponseDtoWithNutrientNames2.get(0).getDrugResponseDtoList().size()).isEqualTo(0);

    }


}