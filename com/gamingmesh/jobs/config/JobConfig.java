/**
 * Jobs Plugin for Bukkit
 * Copyright (C) 2011 Zak Ford <zak.j.ford@gmail.com>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gamingmesh.jobs.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.gamingmesh.jobs.Jobs;
import com.gamingmesh.jobs.JobsPlugin;
import com.gamingmesh.jobs.container.ActionType;
import com.gamingmesh.jobs.container.DisplayMethod;
import com.gamingmesh.jobs.container.Job;
import com.gamingmesh.jobs.container.JobCommands;
import com.gamingmesh.jobs.container.JobConditions;
import com.gamingmesh.jobs.container.JobInfo;
import com.gamingmesh.jobs.container.JobItems;
import com.gamingmesh.jobs.container.JobPermission;
import com.gamingmesh.jobs.resources.jfep.Parser;
import com.gamingmesh.jobs.stuff.ChatColor;

public class JobConfig {
    private JobsPlugin plugin;

    public JobConfig(JobsPlugin plugin) {
	this.plugin = plugin;
    }

    public void reload() throws IOException {
	// job settings
	loadJobSettings();
    }

    /**
     * Method to load the jobs configuration
     * 
     * loads from Jobs/jobConfig.yml
     * @throws IOException 
     */
    @SuppressWarnings("deprecation")
    private void loadJobSettings() throws IOException {
	File f = new File(plugin.getDataFolder(), "jobConfig.yml");
	InputStreamReader s = new InputStreamReader(new FileInputStream(f), "UTF-8");

	ArrayList<Job> jobs = new ArrayList<Job>();
	Jobs.setJobs(jobs);
	Jobs.setNoneJob(null);
	if (!f.exists()) {
	    try {
		f.createNewFile();
	    } catch (IOException e) {
		Jobs.getPluginLogger().severe("Unable to create jobConfig.yml!  No jobs were loaded!");
		s.close();
		return;
	    }
	}
	YamlConfiguration conf = new YamlConfiguration();
	conf.options().pathSeparator('/');
	try {
	    conf.load(s);
	    s.close();
	} catch (Exception e) {
	    Bukkit.getServer().getLogger().severe("==================== Jobs ====================");
	    Bukkit.getServer().getLogger().severe("Unable to load jobConfig.yml!");
	    Bukkit.getServer().getLogger().severe("Check your config for formatting issues!");
	    Bukkit.getServer().getLogger().severe("No jobs were loaded!");
	    Bukkit.getServer().getLogger().severe("Error: " + e.getMessage());
	    Bukkit.getServer().getLogger().severe("==============================================");
	    return;
	}
	//conf.options().header(new StringBuilder().append("Jobs configuration.").append(System.getProperty("line.separator")).append(System.getProperty("line.separator")).append("Stores information about each job.").append(System.getProperty("line.separator")).append(System.getProperty("line.separator")).append("For example configurations, visit http://dev.bukkit.org/bukkit-plugins/jobs-reborn/.").append(System.getProperty("line.separator")).toString());

	ConfigurationSection jobsSection = conf.getConfigurationSection("Jobs");
	//if (jobsSection == null) {
	//	jobsSection = conf.createSection("Jobs");
	//}
	for (String jobKey : jobsSection.getKeys(false)) {
	    ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobKey);
	    String jobName = jobSection.getString("fullname");

	    // Translating unicode
	    jobName = StringEscapeUtils.unescapeJava(jobName);

	    if (jobName == null) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid fullname property. Skipping job!");
		continue;
	    }

	    int maxLevel = jobSection.getInt("max-level", 0);
	    if (maxLevel < 0)
		maxLevel = 0;

	    int vipmaxLevel = jobSection.getInt("vip-max-level", 0);
	    if (vipmaxLevel < 0)
		vipmaxLevel = 0;

	    Integer maxSlots = jobSection.getInt("slots", 0);
	    if (maxSlots.intValue() <= 0) {
		maxSlots = null;
	    }

	    String jobShortName = jobSection.getString("shortname");
	    if (jobShortName == null) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " is missing the shortname property.  Skipping job!");
		continue;
	    }

	    String description = org.bukkit.ChatColor.translateAlternateColorCodes('&', jobSection.getString("description", ""));

	    ChatColor color = ChatColor.WHITE;
	    if (jobSection.contains("ChatColour")) {
		color = ChatColor.matchColor(jobSection.getString("ChatColour", ""));
		if (color == null) {
		    color = ChatColor.WHITE;
		    Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid ChatColour property.  Defaulting to WHITE!");
		}
	    }
	    DisplayMethod displayMethod = DisplayMethod.matchMethod(jobSection.getString("chat-display", ""));
	    if (displayMethod == null) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid chat-display property. Defaulting to None!");
		displayMethod = DisplayMethod.NONE;
	    }

	    Parser maxExpEquation;
	    String maxExpEquationInput = jobSection.getString("leveling-progression-equation");
	    try {
		maxExpEquation = new Parser(maxExpEquationInput);
		// test equation
		maxExpEquation.setVariable("numjobs", 1);
		maxExpEquation.setVariable("joblevel", 1);
		maxExpEquation.getValue();
	    } catch (Exception e) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid leveling-progression-equation property. Skipping job!");
		continue;
	    }

	    Parser incomeEquation;
	    String incomeEquationInput = jobSection.getString("income-progression-equation");
	    try {
		incomeEquation = new Parser(incomeEquationInput);
		// test equation
		incomeEquation.setVariable("numjobs", 1);
		incomeEquation.setVariable("joblevel", 1);
		incomeEquation.setVariable("baseincome", 1);
		incomeEquation.getValue();
	    } catch (Exception e) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid income-progression-equation property. Skipping job!");
		continue;
	    }

	    Parser expEquation;
	    String expEquationInput = jobSection.getString("experience-progression-equation");
	    try {
		expEquation = new Parser(expEquationInput);
		// test equation
		expEquation.setVariable("numjobs", 1);
		expEquation.setVariable("joblevel", 1);
		expEquation.setVariable("baseexperience", 1);
		expEquation.getValue();
	    } catch (Exception e) {
		Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid experience-progression-equation property. Skipping job!");
		continue;
	    }

	    // Gui item
	    ItemStack GUIitem = new ItemStack(Material.getMaterial(35), 1, (byte) 13);
	    if (jobSection.contains("Gui")) {
		ConfigurationSection guiSection = jobSection.getConfigurationSection("Gui");
		if (guiSection.contains("Id") && guiSection.contains("Data") && guiSection.isInt("Id") && guiSection.isInt("Data")) {
		    GUIitem = new ItemStack(Material.getMaterial(guiSection.getInt("Id")), 1, (byte) guiSection.getInt("Data"));
		} else
		    Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid Gui property. Please fix this if you want to use it!");
	    }

	    // Permissions
	    ArrayList<JobPermission> jobPermissions = new ArrayList<JobPermission>();
	    ConfigurationSection permissionsSection = jobSection.getConfigurationSection("permissions");
	    if (permissionsSection != null) {
		for (String permissionKey : permissionsSection.getKeys(false)) {
		    ConfigurationSection permissionSection = permissionsSection.getConfigurationSection(permissionKey);

		    String node = permissionKey.toLowerCase();
		    if (permissionSection == null) {
			Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid permission key" + permissionKey + "!");
			continue;
		    }
		    boolean value = permissionSection.getBoolean("value", true);
		    int levelRequirement = permissionSection.getInt("level", 0);
		    jobPermissions.add(new JobPermission(node, value, levelRequirement));
		}
	    }

	    // Conditions
	    ArrayList<JobConditions> jobConditions = new ArrayList<JobConditions>();
	    ConfigurationSection conditionsSection = jobSection.getConfigurationSection("conditions");
	    if (conditionsSection != null) {
		for (String ConditionKey : conditionsSection.getKeys(false)) {
		    ConfigurationSection permissionSection = conditionsSection.getConfigurationSection(ConditionKey);

		    String node = ConditionKey.toLowerCase();
		    if (permissionSection == null) {
			Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid condition key " + ConditionKey + "!");
			continue;
		    }
		    if (!permissionSection.contains("requires") || !permissionSection.contains("perform")) {
			Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid condition requirement " + ConditionKey + "!");
			continue;
		    }
		    List<String> requires = permissionSection.getStringList("requires");
		    List<String> perform = permissionSection.getStringList("perform");

		    jobConditions.add(new JobConditions(node, requires, perform));
		}
	    }

	    // Command on leave
	    List<String> JobsCommandOnLeave = new ArrayList<String>();
	    if (jobSection.isList("cmd-on-leave")) {
		JobsCommandOnLeave = jobSection.getStringList("cmd-on-leave");
	    }

	    // Command on join
	    List<String> JobsCommandOnJoin = new ArrayList<String>();
	    if (jobSection.isList("cmd-on-join")) {
		JobsCommandOnJoin = jobSection.getStringList("cmd-on-join");
	    }

	    // Commands
	    ArrayList<JobCommands> jobCommand = new ArrayList<JobCommands>();
	    ConfigurationSection commandsSection = jobSection.getConfigurationSection("commands");
	    if (commandsSection != null) {
		for (String commandKey : commandsSection.getKeys(false)) {
		    ConfigurationSection commandSection = commandsSection.getConfigurationSection(commandKey);

		    String node = commandKey.toLowerCase();
		    if (commandSection == null) {
			Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid command key" + commandKey + "!");
			continue;
		    }
		    String command = commandSection.getString("command");
		    int levelFrom = commandSection.getInt("levelFrom");
		    int levelUntil = commandSection.getInt("levelUntil");
		    jobCommand.add(new JobCommands(node, command, levelFrom, levelUntil));
		}
	    }

	    // Items
	    ArrayList<JobItems> jobItems = new ArrayList<JobItems>();
	    ConfigurationSection itemsSection = jobSection.getConfigurationSection("items");
	    if (itemsSection != null) {
		for (String itemKey : itemsSection.getKeys(false)) {
		    ConfigurationSection itemSection = itemsSection.getConfigurationSection(itemKey);

		    String node = itemKey.toLowerCase();
		    if (itemSection == null) {
			Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid item key " + itemKey + "!");
			continue;
		    }
		    int id = itemSection.getInt("id");
		    String name = itemSection.getString("name");

		    List<String> lore = new ArrayList<String>();
		    for (String eachLine : itemSection.getStringList("lore")) {
			lore.add(org.bukkit.ChatColor.translateAlternateColorCodes('&', eachLine));
		    }

		    List<String> enchants = new ArrayList<String>();
		    if (itemSection.getStringList("enchants") != null)
			for (String eachLine : itemSection.getStringList("enchants")) {
			    enchants.add(eachLine);
			}

		    Double moneyBoost = itemSection.getDouble("moneyBoost");
		    Double expBoost = itemSection.getDouble("expBoost");
		    jobItems.add(new JobItems(node, id, name, lore, enchants, moneyBoost, expBoost));
		}
	    }

	    Job job = new Job(jobName, jobShortName, description, color, maxExpEquation, displayMethod, maxLevel, vipmaxLevel, maxSlots, jobPermissions, jobCommand,
		jobConditions, jobItems, JobsCommandOnJoin, JobsCommandOnLeave, GUIitem);

	    for (ActionType actionType : ActionType.values()) {
		ConfigurationSection typeSection = jobSection.getConfigurationSection(actionType.getName());
		ArrayList<JobInfo> jobInfo = new ArrayList<JobInfo>();
		if (typeSection != null) {
		    for (String key : typeSection.getKeys(false)) {
			ConfigurationSection section = typeSection.getConfigurationSection(key);
			String myKey = key;
			String type = null;
			String subType = "";
			String meta = "";
			int id = 0;

			if (myKey.contains("-")) {
			    // uses subType
			    subType = ":" + myKey.split("-")[1];
			    meta = myKey.split("-")[1];
			    myKey = myKey.split("-")[0];
			}

			Material material = Material.matchMaterial(myKey);

			if (material == null)
			    material = Material.getMaterial(myKey.replace(" ", "_").toUpperCase());

			if (material == null) {
			    // try integer method
			    Integer matId = null;
			    try {
				matId = Integer.valueOf(myKey);
			    } catch (NumberFormatException e) {
			    }
			    if (matId != null) {
				material = Material.getMaterial(matId);
				if (material != null) {
				    Jobs.getPluginLogger().warning("Job " + jobKey + " " + actionType.getName() + " is using ID: " + key + "!");
				    Jobs.getPluginLogger().warning("Please use the Material name instead: " + material.toString() + "!");
				}
			    }
			}

			if (material != null) {
			    // Break and Place actions MUST be blocks
			    if (actionType == ActionType.BREAK || actionType == ActionType.PLACE) {
				if (!material.isBlock()) {
				    Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid " + actionType.getName() + " type property: " + key
					+ "! Material must be a block!");
				    continue;
				}
			    }
			    // START HACK
			    /* 
			     * Historically, GLOWING_REDSTONE_ORE would ONLY work as REDSTONE_ORE, and putting
			     * GLOWING_REDSTONE_ORE in the configuration would not work.  Unfortunately, this is 
			     * completely backwards and wrong.
			     * 
			     * To maintain backwards compatibility, all instances of REDSTONE_ORE should normalize
			     * to GLOWING_REDSTONE_ORE, and warn the user to change their configuration.  In the
			     * future this hack may be removed and anybody using REDSTONE_ORE will have their
			     * configurations broken.
			     */
			    if (material == Material.REDSTONE_ORE && actionType == ActionType.BREAK) {
				Jobs.getPluginLogger().warning("Job " + jobKey + " is using REDSTONE_ORE instead of GLOWING_REDSTONE_ORE.");
				Jobs.getPluginLogger().warning("Automatically changing block to GLOWING_REDSTONE_ORE.  Please update your configuration.");
				Jobs.getPluginLogger().warning("In vanilla minecraft, REDSTONE_ORE changes to GLOWING_REDSTONE_ORE when interacted with.");
				Jobs.getPluginLogger().warning("In the future, Jobs using REDSTONE_ORE instead of GLOWING_REDSTONE_ORE may fail to work correctly.");
				material = Material.GLOWING_REDSTONE_ORE;
			    }
			    // END HACK

			    type = material.toString();
			    id = material.getId();
			} else if (actionType == ActionType.KILL || actionType == ActionType.TAME || actionType == ActionType.BREED || actionType == ActionType.MILK) {
			    // check entities
			    EntityType entity = EntityType.fromName(key);
			    if (entity == null) {
				try {
				    entity = EntityType.valueOf(key.toUpperCase());
				} catch (IllegalArgumentException e) {
				}
			    }

			    if (entity != null && entity.isAlive()) {
				type = entity.toString();

				id = entity.getTypeId();
			    }

			    // Just to recognize wither skeleton
			    if (key.equalsIgnoreCase("WitherSkeleton")) {
				type = "WitherSkeleton";
				id = 51;
				meta = "1";
			    }

			    // Just to recognize Zombie Villager
			    if (key.equalsIgnoreCase("ZombieVillager")) {
				type = "ZombieVillager";
				id = 54;
				meta = "1";
			    }

			    // Just to recognize Elder Guardian
			    if (key.equalsIgnoreCase("ElderGuardian")) {
				type = "ElderGuardian";
				id = 68;
				meta = "1";
			    }

			} else if (actionType == ActionType.ENCHANT && material == null) {
			    Enchantment enchant = Enchantment.getByName(myKey);
			    if (enchant != null)
				id = enchant.getId();
			    type = myKey;
			} else if (actionType == ActionType.CUSTOMKILL || actionType == ActionType.SHEAR) {

			    type = myKey;
			}

			if (type == null) {
			    Jobs.getPluginLogger().warning("Job " + jobKey + " has an invalid " + actionType.getName() + " type property: " + key + "!");
			    continue;
			}

			double income = section.getDouble("income", 0.0);
			double experience = section.getDouble("experience", 0.0);

			jobInfo.add(new JobInfo(actionType, id, meta, type + subType, income, incomeEquation, experience, expEquation));
		    }
		}
		job.setJobInfo(actionType, jobInfo);
	    }

	    if (jobKey.equalsIgnoreCase("none")) {
		Jobs.setNoneJob(job);
	    } else {
		jobs.add(job);
	    }
	}
	//try {
	//	conf.save(f);
	//} catch (IOException e) {
	//	e.printStackTrace();
	//}
    }
}
