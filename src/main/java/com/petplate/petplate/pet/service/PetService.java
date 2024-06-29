package com.petplate.petplate.pet.service;

import com.petplate.petplate.common.EmbeddedType.Nutrient;
import com.petplate.petplate.common.EmbeddedType.StandardNutrient;
import com.petplate.petplate.common.response.error.ErrorCode;
import com.petplate.petplate.common.response.error.exception.BadRequestException;
import com.petplate.petplate.common.response.error.exception.NotFoundException;
import com.petplate.petplate.medicalcondition.domain.entity.Allergy;
import com.petplate.petplate.medicalcondition.domain.entity.Disease;
import com.petplate.petplate.medicalcondition.repository.AllergyRepository;
import com.petplate.petplate.medicalcondition.repository.DiseaseRepository;
import com.petplate.petplate.pet.domain.Activity;
import com.petplate.petplate.pet.domain.Neutering;
import com.petplate.petplate.pet.domain.entity.PetAllergy;
import com.petplate.petplate.pet.domain.entity.PetDisease;
import com.petplate.petplate.pet.dto.request.*;
import com.petplate.petplate.pet.dto.response.*;
import com.petplate.petplate.pet.domain.entity.Pet;
import com.petplate.petplate.pet.repository.PetAllergyRepository;
import com.petplate.petplate.pet.repository.PetDiseaseRepository;
import com.petplate.petplate.pet.repository.PetRepository;
import com.petplate.petplate.petdailymeal.domain.entity.DailyMeal;
import com.petplate.petplate.petdailymeal.repository.DailyMealRepository;
import com.petplate.petplate.user.domain.entity.User;
import com.petplate.petplate.user.repository.UserMemberShipRepository;
import com.petplate.petplate.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final UserMemberShipRepository userMemberShipRepository;
    private final AllergyRepository allergyRepository;
    private final PetAllergyRepository petAllergyRepository;
    private final DiseaseRepository diseaseRepository;
    private final PetDiseaseRepository petDiseaseRepository;
    private final DailyMealRepository dailyMealRepository;

    /**
     * 반려견 등록
     *
     * @param userId
     * @param requestDto
     * @return Pet 엔티티
     */
    @Transactional
    public Pet addPet(Long userId, @Valid AddPetRequestDto requestDto) {
        // 해당 pk를 가지는 유저가 존재하지 않는 경우
        User owner = userRepository.findById(userId).orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        // 멤버십이 존재하지 않으면서 두마리 이상의 반려견을 추가하려는 경우 => 예외 발생
        if (!userMemberShipRepository.existsByUserId(userId) && petRepository.existsByOwnerId(userId)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }

        Pet pet = Pet.builder()
                .name(requestDto.getName())
                .age(requestDto.getAge())
                .weight(requestDto.getWeight())
                .activity(requestDto.getActivity())
                .neutering(requestDto.getNeutering())
                .owner(owner)
                .build();

        petRepository.save(pet);

        return pet;
    }

    /**
     * 유저의 모든 펫 반환
     *
     * @param userId
     * @return id, name, age, weight, activity, isNeutering, profileImgPath
     */
    public List<ReadPetResponseDto> getAllPets(Long userId) {
        List<ReadPetResponseDto> responses = new ArrayList<>();

        petRepository.findByOwnerId(userId)
                .forEach(pet -> responses.add(ReadPetResponseDto.from(pet)));
        return responses;
    }

    public ReadPetResponseDto getPet(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);

        ReadPetResponseDto response = ReadPetResponseDto.from(pet);

        return response;
    }

    @Transactional
    public void updatePetInfo(Long userId, @Valid ModifyPetInfoRequestDto requestDto) {
        Pet pet = findPet(userId, requestDto.getPetId());

        pet.updateInfo(requestDto.getName(), requestDto.getAge(), requestDto.getWeight(), requestDto.getActivity(), requestDto.getNeutering());
    }

    @Transactional
    public void updateProfileImg(Long userId,  @Valid ModifyPetProfileImgRequestDto requestDto) {
        Pet pet = findPet(userId, requestDto.getPetId());
        pet.updateProfileImg(requestDto.getProfileImg());
    }

    @Transactional
    public void addPetAllergy(Long userId, @Valid AddPetAllergyRequestDto request) {
        Pet pet = findPet(userId, request.getPetId());
        Allergy allergy = allergyRepository.findById(request.getAllergyId())
                .orElseThrow(() -> new BadRequestException(ErrorCode.BAD_REQUEST));

        PetAllergy petAllergy = PetAllergy.builder()
                .pet(pet).allergy(allergy)
                .build();

        // 이미 동일한 알러지가 등록된 경우
        petAllergyRepository.findByPetId(pet.getId()).forEach(pa-> {
            if(pa.getAllergy().getId().equals(request.getAllergyId())) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
        });

        petAllergyRepository.save(petAllergy);
    }

    @Transactional
    public void addPetDisease(Long userId, @Valid AddPetDiseaseRequestDto request) {
        Pet pet = findPet(userId, request.getPetId());
        Disease disease = diseaseRepository.findById(request.getDiseaseId())
                .orElseThrow(() -> new BadRequestException(ErrorCode.BAD_REQUEST));

        PetDisease petDisease = PetDisease.builder()
                .pet(pet)
                .disease(disease)
                .build();

        // 이미 동일한 질병이 등록된 경우
        petDiseaseRepository.findByPetId(pet.getId()).forEach(pd-> {
            if(pd.getDisease().getId().equals(request.getDiseaseId())) {
                throw new BadRequestException(ErrorCode.BAD_REQUEST);
            }
        });

        petDiseaseRepository.save(petDisease);
    }

    /**
     * 펫이 가진 모든 알러지 반환
     *
     * @param userId
     * @param petId
     * @return 알러지 이름, 알러지 설명
     */
    public List<ReadPetAllergyResponseDto> getAllAllergies(Long userId, Long petId) {
        List<ReadPetAllergyResponseDto> responses = new ArrayList<>();

        Pet pet = findPet(userId, petId);
        petAllergyRepository.findByPetId(petId).forEach(petAllergy -> {
            responses.add(ReadPetAllergyResponseDto.from(petAllergy.getAllergy()));
        });

        return responses;
    }

    /**
     * 펫이 가진 모든 질병 반환
     *
     * @param userId
     * @param petId
     * @return 질병이름, 질병 설명
     */
    public List<ReadPetDiseaseResponseDto> getAllDiseases(Long userId, Long petId) {
        List<ReadPetDiseaseResponseDto> responses = new ArrayList<>();

        Pet pet = findPet(userId, petId);
        petDiseaseRepository.findByPetId(petId).forEach(petDisease -> {
            responses.add(ReadPetDiseaseResponseDto.from(petDisease.getDisease()));
        });

        return responses;
    }

    /**
     * 반려견이 '오늘' 하루 섭취한 영양소 정보를 반환함
     *
     * @param userId
     * @param petId
     * @return 영양소의 이름, 단위, 설명, 섭취량, 최소 적정 섭취량, 최대 적정 섭취량, 최소 섭취량 대비 섭취량 비율, 최소 섭취량 대비 최대 섭취량 비율
     */
    public List<ReadPetNutrientResponseDto> getPetNutrientToday(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);
        LocalDate today = LocalDate.now();

        // 오늘 먹은 식사 내역이 없는 경우 예외 발생 => 이 부분은 그냥 예외가 발생하는 대신 DailyMeal을 생성해도 될듯?
        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, today)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        Nutrient nutrient = dailyMeal.getNutrient();

        double weight = pet.getWeight();
        Activity activity = pet.getActivity();
        Neutering neutering = pet.getNeutering();

        List<ReadPetNutrientResponseDto> responses = createNutrientResponsesDto(nutrient, weight, activity, neutering);
        return responses;
    }

    /**
     * 반려견이 '특정 일자'에 하루 섭취한 영양소 정보를 반환함
     *
     * @param userId
     * @param petId
     * @param date
     * @return 영양소의 이름, 단위, 설명, 섭취량, 최소 적정 섭취량, 최대 적정 섭취량, 최소 섭취량 대비 섭취량 비율, 최소 섭취량 대비 최대 섭취량 비율
     */
    public List<ReadPetNutrientResponseDto> getPetNutrient(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);

        // 오늘 먹은 식사 내역이 없는 경우 예외 발생 => 이 부분은 그냥 예외가 발생하는 대신 DailyMeal을 생성해도 될듯?
        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        Nutrient nutrient = dailyMeal.getNutrient();

        double weight = pet.getWeight();
        Activity activity = pet.getActivity();
        Neutering neutering = pet.getNeutering();


        List<ReadPetNutrientResponseDto> responses = createNutrientResponsesDto(nutrient, weight, activity, neutering);

        return responses;
    }

    private static List<ReadPetNutrientResponseDto> createNutrientResponsesDto(Nutrient nutrient, double weight, Activity activity, Neutering neutering) {
        List<ReadPetNutrientResponseDto> responses = new ArrayList<>();

        // 탄수화물 정보
        String carbonName = StandardNutrient.CARBON_HYDRATE.getName();
        String carbonUnit = StandardNutrient.CARBON_HYDRATE.getUnit();
        String carbonDescription = StandardNutrient.CARBON_HYDRATE.getDescription();
        double carbonAmount = nutrient.getCarbonHydrate();
        double carbonMinimumIntake = StandardNutrient.calculateProperCarbonHydrateAmount(weight, activity, neutering);
        double carbonMaximumIntake = StandardNutrient.calculateProperMaximumCarbonHydrateAmount(weight, activity, neutering);
        responses.add(ReadPetNutrientResponseDto.of(carbonName, carbonUnit, carbonDescription, carbonAmount, carbonMinimumIntake, carbonMaximumIntake));

        // 단백질 정보
        String proteinName = StandardNutrient.PROTEIN.getName();
        String proteinUnit = StandardNutrient.PROTEIN.getUnit();
        String proteinDescription = StandardNutrient.PROTEIN.getDescription();
        double proteinAmount = nutrient.getProtein();
        double proteinMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.PROTEIN, weight);
        double proteinMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.PROTEIN, weight);
        responses.add(ReadPetNutrientResponseDto.of(proteinName, proteinUnit, proteinDescription, proteinAmount, proteinMinimumIntake, proteinMaximumIntake));

        // 지방 정보
        String fatName = StandardNutrient.FAT.getName();
        String fatUnit = StandardNutrient.FAT.getUnit();
        String fatDescription = StandardNutrient.FAT.getDescription();
        double fatAmount = nutrient.getFat();
        double fatMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.FAT, weight);
        double fatMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.FAT, weight);
        responses.add(ReadPetNutrientResponseDto.of(fatName, fatUnit, fatDescription, fatAmount, fatMinimumIntake, fatMaximumIntake));

        // 칼슘 정보
        String calciumName = StandardNutrient.CALCIUM.getName();
        String calciumUnit = StandardNutrient.CALCIUM.getUnit();
        String calciumDescription = StandardNutrient.CALCIUM.getDescription();
        double calciumAmount = nutrient.getCalcium();
        double calciumMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.CALCIUM, weight);
        double calciumMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.CALCIUM, weight);
        responses.add(ReadPetNutrientResponseDto.of(calciumName, calciumUnit, calciumDescription, calciumAmount, calciumMinimumIntake, calciumMaximumIntake));

        // 인 정보
        String phosphorusName = StandardNutrient.PHOSPHORUS.getName();
        String phosphorusUnit = StandardNutrient.PHOSPHORUS.getUnit();
        String phosphorusDescription = StandardNutrient.PHOSPHORUS.getDescription();
        double phosphorusAmount = nutrient.getPhosphorus();
        double phosphorusMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.PHOSPHORUS, weight);
        double phosphorusMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.PHOSPHORUS, weight);
        responses.add(ReadPetNutrientResponseDto.of(phosphorusName, phosphorusUnit, phosphorusDescription, phosphorusAmount, phosphorusMinimumIntake, phosphorusMaximumIntake));

        // 비타민A 정보
        String vitaminAName = StandardNutrient.VITAMIN_A.getName();
        String vitaminAUnit = StandardNutrient.VITAMIN_A.getUnit();
        String vitaminADescription = StandardNutrient.VITAMIN_A.getDescription();
        double vitaminAAmount = nutrient.getVitamin().getVitaminA();
        double vitaminAMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.VITAMIN_A, weight);
        double vitaminAMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.VITAMIN_A, weight);
        responses.add(ReadPetNutrientResponseDto.of(vitaminAName, vitaminAUnit, vitaminADescription, vitaminAAmount, vitaminAMinimumIntake, vitaminAMaximumIntake));

        // 비타민D 정보
        String vitaminDName = StandardNutrient.VITAMIN_D.getName();
        String vitaminDUnit = StandardNutrient.VITAMIN_D.getUnit();
        String vitaminDDescription = StandardNutrient.VITAMIN_D.getDescription();
        double vitaminDAmount = nutrient.getVitamin().getVitaminD();
        double vitaminDMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.VITAMIN_D, weight);
        double vitaminDMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.VITAMIN_D, weight);
        responses.add(ReadPetNutrientResponseDto.of(vitaminDName, vitaminDUnit, vitaminDDescription, vitaminDAmount, vitaminDMinimumIntake, vitaminDMaximumIntake));

        // 비타민E 정보
        String vitaminEName = StandardNutrient.VITAMIN_E.getName();
        String vitaminEUnit = StandardNutrient.VITAMIN_E.getUnit();
        String vitaminEDescription = StandardNutrient.VITAMIN_E.getDescription();
        double vitaminEAmount = nutrient.getVitamin().getVitaminE();
        double vitaminEMinimumIntake = StandardNutrient.calculateProperNutrientAmount(StandardNutrient.VITAMIN_E, weight);
        double vitaminEMaximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(StandardNutrient.VITAMIN_E, weight);
        responses.add(ReadPetNutrientResponseDto.of(vitaminEName, vitaminEUnit, vitaminEDescription, vitaminEAmount, vitaminEMinimumIntake, vitaminEMaximumIntake));

        return responses;
    }

    /**
     * 반려견이 '오늘' 섭취한 영양소를 적정 섭취량에 대한 비율로 반환함
     * 예) 체중에 대해서 계산한 단백질 적정량이 100g인데 총 200g을 섭취한 경우 protein = 2가 반환
     *
     * @param userId
     * @param petId
     * @return 영양소 비율
     */
    public List<ReadPetNutrientRatioResponseDto> getPetNutrientRatioToday(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);
        LocalDate today = LocalDate.now();

        // 오늘 먹은 식사 내역이 없는 경우 예외 발생 => 이 부분은 그냥 예외가 발생하는 대신 DailyMeal을 생성해도 될듯?
        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, today)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        List<ReadPetNutrientRatioResponseDto> responses = new ArrayList<>();

        // 비율 계산
        Map<StandardNutrient, Double> nutrientsMap =
                StandardNutrient.getNutrientsMap(dailyMeal.getNutrient(), pet.getWeight(), pet.getActivity(), pet.getNeutering());

        nutrientsMap.forEach((nutrient, ratio) -> {
            responses.add(ReadPetNutrientRatioResponseDto.of(nutrient.getName(), ratio));
        });

        return responses;
    }

    /**
     * 반려견이 '특정 일자'에 섭취한 영양소를 적정 섭취량에 대한 비율로 반환함
     * 예) 체중에 대해서 계산한 단백질 적정량이 100g인데 총 200g을 섭취한 경우 protein = 2가 반환
     *
     * @param userId
     * @param petId
     * @param date
     * @return 영양소 비율
     */
    public List<ReadPetNutrientRatioResponseDto> getPetNutrientRatio(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        List<ReadPetNutrientRatioResponseDto> responses = new ArrayList<>();

        // 비율 계산
        Map<StandardNutrient, Double> nutrientsMap =
                StandardNutrient.getNutrientsMap(dailyMeal.getNutrient(), pet.getWeight(), pet.getActivity(), pet.getNeutering());

        nutrientsMap.forEach((nutrient, ratio) -> {
            responses.add(ReadPetNutrientRatioResponseDto.of(nutrient.getName(), ratio));
        });

        return responses;
    }

    /**
     * '특정일자'에 과잉 섭취한 영양소를 반환
     *
     * @param userId
     * @param petId
     * @param date
     * @return 영양소의 이름, 단위, 설명, 섭취량, 최소 적정 섭취량, 최대 적정 섭취량, 최소 섭취량 대비 섭취량 비율, 최소 섭취량 대비 최대 섭취량 비율
     */
    public List<ReadPetNutrientResponseDto> getSufficientNutrient(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);
        double weight = pet.getWeight();
        Activity activity = pet.getActivity();
        Neutering neutering = pet.getNeutering();

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        List<ReadPetNutrientResponseDto> responses = new ArrayList<>();

        StandardNutrient.findSufficientNutrients(dailyMeal.getNutrient(), weight, activity, neutering)
                .forEach(nutrient -> {
                    double amount = dailyMeal.getNutrient().getNutrientAmountByName(nutrient.getName());
                    double minimumIntake = StandardNutrient.calculateProperNutrientAmount(nutrient, weight);
                    double maximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(nutrient, weight);

                    if (nutrient.equals(StandardNutrient.CARBON_HYDRATE)) {
                        minimumIntake = StandardNutrient.calculateProperCarbonHydrateAmount(weight, activity, neutering);
                        maximumIntake = StandardNutrient.calculateProperMaximumCarbonHydrateAmount(weight, activity, neutering);
                    }

                    responses.add(ReadPetNutrientResponseDto.of(nutrient.getName(), nutrient.getUnit(), nutrient.getDescription(), amount, minimumIntake, maximumIntake));
                });

        return responses;
    }

    /**
     * '특정일자'에 부족 섭취한 영양소를 반환
     *
     * @param userId
     * @param petId
     * @param date
     * @return 영양소의 이름, 단위, 설명, 섭취량, 최소 적정 섭취량, 최대 적정 섭취량, 최소 섭취량 대비 섭취량 비율, 최소 섭취량 대비 최대 섭취량 비율
     */
    public List<ReadPetNutrientResponseDto> getDeficientNutrient(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);
        double weight = pet.getWeight();
        Activity activity = pet.getActivity();
        Neutering neutering = pet.getNeutering();

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        List<ReadPetNutrientResponseDto> responses = new ArrayList<>();

        StandardNutrient.findDeficientNutrients(dailyMeal.getNutrient(), weight, activity, neutering)
                .forEach(nutrient -> {
                    double amount = dailyMeal.getNutrient().getNutrientAmountByName(nutrient.getName());
                    double minimumIntake = StandardNutrient.calculateProperNutrientAmount(nutrient, weight);
                    double maximumIntake = StandardNutrient.calculateProperMaximumNutrientAmount(nutrient, weight);

                    if (nutrient.equals(StandardNutrient.CARBON_HYDRATE)) {
                        minimumIntake = StandardNutrient.calculateProperCarbonHydrateAmount(weight, activity, neutering);
                        maximumIntake = StandardNutrient.calculateProperMaximumCarbonHydrateAmount(weight, activity, neutering);
                    }

                    responses.add(ReadPetNutrientResponseDto.of(nutrient.getName(), nutrient.getUnit(), nutrient.getDescription(), amount, minimumIntake, maximumIntake));
                });

        return responses;
    }

    /**
     * 반려견이 오늘 먹은 총 칼로리 반환
     *
     * @param userId
     * @param petId
     * @return 섭취 칼로리
     */
    public ReadPetKcalResponseDto getPetKcalToday(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);
        LocalDate today = LocalDate.now();

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, today)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        return ReadPetKcalResponseDto.of(dailyMeal.getKcal());
    }

    /**
     * 반려견이 '특정 일자'에 먹은 총 칼로리 반환
     *
     * @param userId
     * @param petId
     * @param date
     * @return 섭취 칼로리
     */
    public ReadPetKcalResponseDto getPetKcal(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        return ReadPetKcalResponseDto.of(dailyMeal.getKcal());
    }

    /**
     * 반려견이 먹어야할 적정 칼로리 반환
     *
     * @param userId
     * @param petId
     * @return 적정 칼로리
     */
    public ReadPetKcalResponseDto getPetProperKcal(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);

        double properKcal = pet.getProperKcal();

        return ReadPetKcalResponseDto.of(properKcal);
    }

    /**
     * 반려견의 적정 섭취 칼로리 대비 '오늘' 섭취한 칼로리에 대한 비율을 반환함
     *
     * @param userId
     * @param petId
     * @return 적정 섭취 칼로리 대비 섭취 칼로리 비율
     */
    public ReadPetKcalRatioResponseDto getPetKcalRatioToday(Long userId, Long petId) {
        Pet pet = findPet(userId, petId);
        LocalDate today = LocalDate.now();

        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, today)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        double properKcal = pet.getProperKcal();

        double properKcalRatio = dailyMeal.getKcal() / properKcal;

        return ReadPetKcalRatioResponseDto.of(properKcalRatio);
    }

    /**
     * 반려견의 적정 섭취 칼로리 대비 '특정 일자'에 섭취한 칼로리에 대한 비율을 반환함
     *
     * @param userId
     * @param petId
     * @param date
     * @return 적정 섭취 칼로리 대비 섭취 칼로리 비율
     */
    public ReadPetKcalRatioResponseDto getPetKcalRatio(Long userId, Long petId, LocalDate date) {
        Pet pet = findPet(userId, petId);
        DailyMeal dailyMeal = dailyMealRepository.findByPetIdAndCreatedAt(petId, date)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        double properKcal = pet.getProperKcal();

        double properKcalRatio = dailyMeal.getKcal() / properKcal;

        return ReadPetKcalRatioResponseDto.of(properKcalRatio);
    }


    private Pet findPet(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new NotFoundException(ErrorCode.NOT_FOUND));

        // 조회하려는 반려견이 본인의 반려견이 아닌 경우 예외 발생
        if (!pet.getOwner().getId().equals(userId)) {
            throw new BadRequestException(ErrorCode.BAD_REQUEST);
        }

        return pet;
    }
}
