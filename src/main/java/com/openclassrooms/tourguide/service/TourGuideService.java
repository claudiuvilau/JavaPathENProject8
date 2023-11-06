package com.openclassrooms.tourguide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.model.AttractionsNearUser;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tripPricer.Provider;
import tripPricer.TripPricer;

@Service
public class TourGuideService {
	private Logger logger = LoggerFactory.getLogger(TourGuideService.class);
	private final GpsUtil gpsUtil;
	private final RewardsService rewardsService;
	private final TripPricer tripPricer = new TripPricer();
	public final Tracker tracker;
	boolean testMode = true;

	public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
		this.gpsUtil = gpsUtil;
		this.rewardsService = rewardsService;

		Locale.setDefault(Locale.US);

		if (testMode) {
			logger.info("TestMode enabled");
			logger.debug("Initializing users");
			initializeInternalUsers();
			logger.debug("Finished initializing users");
		}
		tracker = new Tracker(this);
		addShutDownHook();
	}

	public List<UserReward> getUserRewards(User user) {
		return user.getUserRewards();
	}

	public VisitedLocation getUserLocation(User user) throws InterruptedException, ExecutionException {
		VisitedLocation visitedLocation = (user.getVisitedLocations().size() > 0) ? user.getLastVisitedLocation()
				: trackUserLocation(user);
		return visitedLocation;
	}

	public User getUser(String userName) {
		return internalUserMap.get(userName);
	}

	public List<User> getAllUsers() {
		return internalUserMap.values().stream().collect(Collectors.toList());
	}

	public void addUser(User user) {
		if (!internalUserMap.containsKey(user.getUserName())) {
			internalUserMap.put(user.getUserName(), user);
		}
	}

	public List<Provider> getTripDeals(User user) {
		int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(i -> i.getRewardPoints()).sum();
		List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(),
				user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(),
				user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
		user.setTripDeals(providers);
		return providers;
	}

	public VisitedLocation trackUserLocation(User user)
			throws InterruptedException, ExecutionException {

		// VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
		// user.addToVisitedLocations(visitedLocation);

		// CompletableFuture<VisitedLocation> completableFuture = new
		// CompletableFuture<>();

		// Executors.newCachedThreadPool().submit(() -> {
		// VisitedLocation visitedLocationFuture =
		// gpsUtil.getUserLocation(user.getUserId());
		// user.addToVisitedLocations(visitedLocationFuture);
		// rewardsService.calculateRewards(user);
		// completableFuture.complete(visitedLocationFuture);
		// return visitedLocationFuture;
		// });

		// rewardsService.calculateRewards(user);
		// return visitedLocation;
		// return completableFuture.get();

		ExecutorService executor = Executors.newFixedThreadPool(1000);

		CompletableFuture<VisitedLocation> completableFuture = CompletableFuture.supplyAsync(() -> {
			VisitedLocation visitedLocationFuture = gpsUtil.getUserLocation(user.getUserId());
			user.addToVisitedLocations(visitedLocationFuture);
			try {
				rewardsService.calculateRewards(user);
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return visitedLocationFuture;
		}, executor);

		return completableFuture.get();

	}

	private VisitedLocation testCalculateRewards(User user) throws InterruptedException, ExecutionException {
		VisitedLocation visitedLocationFuture = gpsUtil.getUserLocation(user.getUserId());
		user.addToVisitedLocations(visitedLocationFuture);
		rewardsService.calculateRewards(user);
		return visitedLocationFuture;
	}

	public List<AttractionsNearUser> getNearByAttractions(VisitedLocation visitedLocation) {
		// List<Attraction> nearbyAttractions = new ArrayList<>();
		List<AttractionsNearUser> nearbyAttractions = new ArrayList<>();

		Map<Double, Attraction> mapDoubleAttraction = new HashMap<>();

		double distanceAttrToVisited;
		for (Attraction attraction : gpsUtil.getAttractions()) {
			distanceAttrToVisited = rewardsService.getDistance(attraction, visitedLocation.location);
			mapDoubleAttraction.put(distanceAttrToVisited, attraction);
		}

		TreeMap<Double, Attraction> sortedMapDoubleAttraction = new TreeMap<>(mapDoubleAttraction);

		AttractionsNearUser attractionsNearUser;
		int rewardPoints;
		RewardCentral rewardCentral = new RewardCentral();

		// only 5 tourist attractions to the user
		int attrNearUser = 5;
		for (Map.Entry<Double, Attraction> entrySortedMapDoubleAttraction : sortedMapDoubleAttraction.entrySet()) {
			double keyDistance = entrySortedMapDoubleAttraction.getKey();
			Attraction valueAttraction = entrySortedMapDoubleAttraction.getValue();
			attractionsNearUser = new AttractionsNearUser();
			attractionsNearUser.setAttractionName(valueAttraction.attractionName);
			attractionsNearUser.setDistanceMiles(keyDistance);
			attractionsNearUser.setLatitudeAttraction(valueAttraction.latitude);
			attractionsNearUser.setLongitudeAttraction(valueAttraction.longitude);
			attractionsNearUser.setLatitudeUser(visitedLocation.location.latitude);
			attractionsNearUser.setLongitudeUser(visitedLocation.location.longitude);

			rewardPoints = rewardCentral.getAttractionRewardPoints(valueAttraction.attractionId,
					visitedLocation.userId);
			attractionsNearUser.setRewardPoints(rewardPoints);

			nearbyAttractions.add(attractionsNearUser);
			if (nearbyAttractions.size() == attrNearUser) {
				break; // if the attrNearUser number => exit for
			}

		}

		// if (rewardsService.isWithinAttractionProximity(attraction,
		// visitedLocation.location)) {
		// nearbyAttractions.add(attraction);
		// }

		return nearbyAttractions;
	}

	private void addShutDownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				tracker.stopTracking();
			}
		});
	}

	/**********************************************************************************
	 * 
	 * Methods Below: For Internal Testing
	 * 
	 **********************************************************************************/
	private static final String tripPricerApiKey = "test-server-api-key";
	// Database connection will be used for external users, but for testing purposes
	// internal users are provided and stored in memory
	private final Map<String, User> internalUserMap = new HashMap<>();

	private void initializeInternalUsers() {
		IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
			String userName = "internalUser" + i;
			String phone = "000";
			String email = userName + "@tourGuide.com";
			User user = new User(UUID.randomUUID(), userName, phone, email);
			generateUserLocationHistory(user);

			internalUserMap.put(userName, user);
		});
		logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
	}

	private void generateUserLocationHistory(User user) {
		IntStream.range(0, 3).forEach(i -> {
			user.addToVisitedLocations(new VisitedLocation(user.getUserId(),
					new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
		});
	}

	private double generateRandomLongitude() {
		double leftLimit = -180;
		double rightLimit = 180;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private double generateRandomLatitude() {
		double leftLimit = -85.05112878;
		double rightLimit = 85.05112878;
		return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
	}

	private Date getRandomTime() {
		LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
		return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
	}

}
