package algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import db.DBConnection;
import db.DBConnectionFactory;
import entity.Item;
import external.TicketMasterAPI;

public class GeoRecommendation {
	public List<Item> recommendItems(String userId, double lat, double lon) {
		List<Item> recommendedItems = new ArrayList<>();
		DBConnection conn = DBConnectionFactory.getDBConnection();	//getDBConnection为static所以不用创建对象

		// step 1 Get all favorited items
		Set<String> favoriteItems = conn.getFavoriteItemIds(userId);
		
        // step 2 Get all categories of favorited items, sort by count
		Map<String, Integer> allCategories = new HashMap<>(); // step 2
		for (String item : favoriteItems) {
			Set<String> categories = conn.getCategories(item); // db queries
			for (String category : categories) {
				if (allCategories.containsKey(category)) {
					allCategories.put(category, allCategories.get(category) + 1);
				} else {
					allCategories.put(category, 1);
				}
			}
		}
		List<Entry<String, Integer>> categoryList = new ArrayList<Entry<String, Integer>>(allCategories.entrySet());
		Collections.sort(categoryList, new Comparator<Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				//Entry可以有getValue()和getKey()
				return Integer.compare(o2.getValue(), o1.getValue());
			}
		});
		
		
		//visitedItems和filteredItem具有排重的效果
		Set<Item> visitedItems = new HashSet<>();
		for (Entry<String, Integer> category : categoryList) {
			List<Item> items = conn.searchItems(lat, lon, category.getKey());
			List<Item> filteredItems = new ArrayList<>();
			for (Item item : items) {
				//第一部分排重收藏的部分，第二排重前面素有的部分
				if (!favoriteItems.contains(item.getItemId()) && !visitedItems.contains(item)) {
					filteredItems.add(item);
				}
			}
			Collections.sort(filteredItems, new Comparator<Item>() {
				@Override
				public int compare(Item item1, Item item2) {
					// return the increasing order of distance.
					return Double.compare(item1.getDistance(), item2.getDistance());
				}
			});
			visitedItems.addAll(items);
			recommendedItems.addAll(filteredItems);
		}
		return recommendedItems;		
	}
}
