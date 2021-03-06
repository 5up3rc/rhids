package edu.vt.rhids.main;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import edu.vt.rhids.common.IndexMap;
import edu.vt.rhids.output.Statistics;
import edu.vt.rhids.output.Summary;
import edu.vt.rhids.util.Logger;
import edu.vt.rhids.util.Logger.Verbosity;

/**
 * Resilient Host-based Intrusion Detection System
 * 
 * @author AmrAbed
 *
 */
public class RHIDS
{
	public static IndexMap indexMap;

	private static boolean isUnderAttack;
	private static boolean isDoneTraining;

	public static void main(String[] args)
	{
		Logger.log(run(parseArguments(args)), Verbosity.NONE);
	}

	public static ArrayList<Statistics> run(Parameters p)
	{
		Logger.log(p, Verbosity.NONE);
		final Summary summary = new Summary();
		try
		{
			indexMap = new IndexMap(new BufferedReader(new FileReader("syscalls")));
			Logger.log(indexMap, Verbosity.ALL);
			Logger.emphasize("Number of distinct system calls: " + indexMap.size(), Verbosity.LOW);

			for (int epochSize = p.epochSize.min; epochSize <= p.epochSize.max; epochSize += p.epochSize.step)
			{
				for (float trainThreshold = p.trainThreshold.min; trainThreshold <= p.trainThreshold.max; trainThreshold += p.trainThreshold.step)
				{
					for (int testThreshold = p.testThreshold.min; testThreshold <= p.testThreshold.max; testThreshold += p.testThreshold.step)
					{
						isDoneTraining = false;
						isUnderAttack = false;

						Logger.log("\n#######################################################", Verbosity.LOW);
						Logger.log("Epoch size: " + epochSize, Verbosity.LOW);
						Logger.log("Similarity threshold: " + trainThreshold, Verbosity.LOW);
						Logger.log("Test threshold: " + testThreshold, Verbosity.LOW);
						Logger.log("#######################################################", Verbosity.LOW);

						final BufferedReader reader = new BufferedReader(new FileReader(p.inputFile));
						final Statistics stats = new Statistics(epochSize, trainThreshold, testThreshold);
						final Classifier classifier = new Classifier(reader, stats, p.databaseFile);

						if (classifier.trainUnconditionally())
						{
							classifier.test();
							stats.print();
							summary.add(stats);
						}
						Logger.log(summary, Verbosity.LOW);
						;
					}
				}
			}
		}
		catch (IOException e)
		{
			System.err.println(e);
			System.exit(-2);
		}
		return summary;
	}

	private static Parameters parseArguments(String[] args)
	{
		final Parameters parameters = new Parameters();
		final Options options = createOptions();

		try
		{
			CommandLine command = new BasicParser().parse(options, args);

			if (command.hasOption("help"))
			{
				new HelpFormatter().printHelp("RHIDS", options);
				System.exit(0);
			}

			if (command.hasOption("verbose"))
			{
				Logger.setLevel(Integer.parseInt(command.getOptionValue("verbose")));
			}

			if (command.hasOption("output-file"))
			{
				Logger.setHandler(command.getOptionValue("output-file"));
			}

			if (command.hasOption("database-file"))
			{
				parameters.setDatabaseFilePath(command.getOptionValue("database-file"));
			}

			parameters.setNormalFilePath(command.getOptionValue("input-file"));
			parameters.setEpochSize(command.getOptionValue("epoch-size"));
			parameters.setTrainThreshold(command.getOptionValue("train-threshold"));
			parameters.setTestThreshold(command.getOptionValue("detection-threshold"));
		}
		catch (ParseException | FileNotFoundException e)
		{
			System.err.println("Parsing failed: " + e.getMessage());
			System.exit(-1);
		}

		return parameters;
	}

	private static Options createOptions()
	{

		final Options options = new Options();

		final OptionGroup group = new OptionGroup();
		group.addOption(new Option("h", "help", false, "Print this help message"));
		group.addOption(new Option("i", "input-file", true, "Input file path"));
		group.setRequired(true);

		options.addOptionGroup(group);

		options.addOption("b", "database-file", true, "File to read database from");
		options.addOption("e", "epoch-size", true, "Range for epoch size (default " + Parameters.DEFAULT_EPOCH_SIZE
				+ ")");
		options.addOption("t", "train-threshold", true, "Range for training threshold (default "
				+ Parameters.DEFAULT_TRAIN_THRESHOLD + ")");
		options.addOption("d", "detection-threshold", true, "Range for detection threshold (default "
				+ Parameters.DEFAULT_TEST_THRESHOLD + ")");
		options.addOption("v", "verbose", true, "Verbose level (default 0)");
		options.addOption("o", "output-file", true, "Output file path");

		return options;
	}

	public static boolean isUnderAttack()
	{
		return isUnderAttack;
	}

	public static void setUnderAttack(boolean isUnderAttack)
	{
		RHIDS.isUnderAttack = isUnderAttack;
	}

	public static boolean isDoneTraining()
	{
		return isDoneTraining;
	}

	public static void setDoneTraining(boolean doneTraining)
	{
		RHIDS.isDoneTraining = doneTraining;
	}
}
