# Rule-based RE

## About

Rule-based Relation Extraction Model

## prerequisite
* `java 1.8`
* `maven`

## How to use
refer `Main.java`

* --generate-patterns: raw-pattern extraction
* --annotate-patterns: generate positive/negative patterns from the human feecback
* --extract-relations: extract triples from the input sentence

### How to run
`mvn exec:java -Dexec.args="--extract-relations"`

Extracts relations from DOCUMENT to EXTRACTS in the 'FromTheS.conf' file.

## Licenses
* `CC BY-NC-SA` [Attribution-NonCommercial-ShareAlike](https://creativecommons.org/licenses/by-nc-sa/2.0/)
* If you want to commercialize this resource, [please contact to us](http://mrlab.kaist.ac.kr/contact)

## Maintainer
Sangha Nam `nam.sangha@kaist.ac.kr`

## Publisher
[Machine Reading Lab](http://mrlab.kaist.ac.kr/) @ KAIST

## Citation
@inproceedings{choi2016filling,
  title={Filling a Knowledge Graph with a Crowd},
  author={Choi, GyuHyeon and Nam, Sangha and Choi, Dongho and Choi, Key-Sun},
  booktitle={Proceedings of the Open Knowledge Base and Question Answering Workshop (OKBQA 2016)},
  pages={67--71},
  year={2016}
}
