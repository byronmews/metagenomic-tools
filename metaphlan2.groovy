// Metaphlan2 PE run for qc passed and screened reads
//
// Usage: bpipe run metaphlan2_run.py *
// Author: Graham Rose
//

// Requires: metaphlan_to_stamp.pl (from https://github.com/mlangill/microbiome_helper/)

METAPHLAN_DB="/srv/data0/dbs/metaphlan2/db_v20/mpa_v20_m200.pkl"
BOWTIE2DB="/srv/data0/dbs/metaphlan2/db_v20/mpa_v20_m200"


// Profile all screen pipeline qc passed fastq files in folder
metaphlan={
	
	doc "Run metaphlan2"	

	input_extension = ".fastq"

	products = [
	("$input1".replaceAll(/_L001_.*/,"") - input_extension + '.bowtie2.bz2'),
	("$input1".replaceAll(/_L001_.*/,"") - input_extension + '.profiled_metagenome.txt')	
	]
	
	produce(products) {

		exec """
		metaphlan2.py $input1.fastq,$input2.fastq 
		--mpa_pkl $METAPHLAN_DB 
		--bowtie2db $BOWTIE2DB
		--input_type fastq 
		--bowtie2out $output1.bowtie2.bz2
		--nproc 10
		-o $output2.profiled_metagenome.txt
		"""
	}
	forward output2.profiled_metagenome.txt
}

// Merge all output tables, use wildcard handled by merge script
merge_tables={	

	doc "Metaphlan utils merge abundances from all samples"
	
	produce("merged_abundance_table.txt") {
		exec "merge_metaphlan_tables.py *.profiled_metagenome.txt >> $output.txt"
	}
}

// Basic heatmaps of merged tables
heatmap={
	
	doc = "Metaphlan utils cluster samples"

	// Map holding key-value pairs for a few options to use below
	def levels = ['s':'species', 'g':'genus', 'f':'family', 'p':'phylum']	

	// Output dir
	output.dir = "heatmaps"
	
	// Produce filenames using above map values
	produce("*") {

		levels.each { id, tax_level ->

			exec """
			metaphlan_hclust_heatmap.py
			--in $input.txt
			-m average
			-d braycurtis
			-f correlation
			--minv 0
			-c bbcry
			--top 25
			--tax_lev ${id}
			--out ${output}.${tax_level}.png
			"""
		}
	}
	forward input.profiled_metagenome.txt
}

// Output table in STAMP spf format using mlangill included script
convert_to_stamp={

	doc "Convert combined profile output to STAMP ready format"

	transform(".txt") to (".stamp.spf") {

		exec "perl metaphlan_to_stamp.pl $input.profiled_metagenome.txt > $output.stamp.spf"	
	
	}
}


// Multiple samples where file names begin with sample name separated by regex
// such as: 1-2_S2_L001_R1_001.fastq)
Bpipe.run {
	"%_S*.fastq" * [ metaphlan ] + merge_tables + heatmap + convert_to_stamp

}




