import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Normal-behavior Database
 *
 * @author AmrAbed
 *
 */
public class Database extends HashMap<BoSC, Long>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Create normal-behavior database from a dump file
	 *
	 * @param filePath
	 *            path to the dump file
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public Database(String filePath) throws NumberFormatException, IOException
	{
		if (filePath != null)
		{
			final BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line;
			while ((line = reader.readLine()) != null)
			{
				final String[] words = line.split("\t");
				put(new BoSC(words[0]), Long.parseLong(words[1]));
			}
			reader.close();
		}
	}

	public void add(BoSC bosc)
	{
		if (bosc == null)
		{
			return;
		}

		if (containsKey(bosc))
		{
			replace(bosc, get(bosc) + 1);
		}
		else
		{
			put(bosc, 1L);
		}
	}

	public void commit(Database change)
	{
		for (BoSC bosc : change.keySet())
		{
			long value = change.get(bosc);
			if (this.containsKey(bosc))
			{
				value += this.get(bosc);
			}
			this.put(bosc, value);
		}
	}

	private double getNorm()
	{
		double norm = 0;
		for (BoSC bosc : keySet())
		{
			long val = get(bosc);
			norm += val * val;
		}
		return Math.sqrt(norm);
	}

	public double calculateSimilarity(Database other)
	{
		if (this.isEmpty() || other.isEmpty())
		{
			return 0;
		}

		double dot = 0;
		double norm1 = this.getNorm(), norm2 = other.getNorm();
		for (BoSC bosc : keySet())
		{
//			System.out.print(bosc + " -> ");
			if (other.containsKey(bosc))
			{
				long value1 = other.get(bosc);
				long value2 = this.get(bosc);
//                                System.out.print("found (Frequencies: " + value2 + " & " + value1 + ")");
				dot += value1 * value2;
			}
		}
		return dot / (norm1 * norm2);
	}

	public void dump(String file) throws FileNotFoundException
	{
		try (PrintStream out = new PrintStream(file))
		{
			for (BoSC bosc : keySet())
			{
				out.println(bosc + "\t" + get(bosc));
			}
		}
	}

	@Override
	public String toString()
	{
		String output = new String();
		for (ArrayList<Byte> entry : keySet())
		{
			output += entry + " => " + get(entry) + "\n";
		}
		return output;
	}
}
