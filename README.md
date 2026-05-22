# Dual Approach for 3D Ki67 Classification Workflow

## Overview

This repository contains a **custom Fiji/ImageJ Groovy script** for the quantitative analysis of 3D confocal microscopy images. The workflow is designed to quantify **Ki67 proliferation signal within DAPI-segmented nuclei** using precomputed **Cellpose segmentation masks**.

The pipeline performs:
- 3D object-based analysis
- Per-nucleus signal quantification
- Dual classification of Ki67 positivity:
  - Intensity-based threshold
  - Spatial overlap (colocalization)

This approach enables **robust single-cell quantification in 3D image datasets**, beyond standard GUI-based workflows.

---

## Requirements

### Software
- **Fiji/ImageJ** (recommended distribution)
- Java 8 or higher

### Required plugins/libraries
Make sure Fiji includes:
- **Bio-Formats**
- **MCIB3D / 3D ImageJ Suite**
- Groovy scripting support

Optional:
- Cellpose (used for segmentation prior to this analysis)

---

## Input Data Structure
The script expects the following directory organization:
```
project/
│
├── images/        → Raw multichannel .tif images
├── nuclei/        → DAPI masks (Cellpose output)
├── ki67/          → Ki67 masks (Cellpose output)
└── csv/           → Output directory
```
### Naming conventions
**⚠️ File names must match across folders.**
- Raw images: `image_name.tif`
- Segmentation masks: `image_name_cp_masks.tif`


## How to Run the Script

### 1. Open Fiji
- ``Plugins → New → Script``
### 2. Set scripting language
- ``Groovy``
### 3. Configure paths

Edit the following variables in the script:

```groovy
def inputFilesRawDir = new File("path/to/images")
def inputFilesNucleiDir = new File("path/to/nuclei")
def inputFilesKi67Dir = new File("path/to/ki67")
def outputDir = new File("path/to/csv")
```
### 4. Run the script
- Click ``Run``

## Outputs
### Per-image results
``3DAnalysis_perImage_new.csv``

- Contains:
```
Image name
Number of DAPI+ nuclei
Number of Ki67+ nuclei (threshold)
Number of Ki67− nuclei (threshold)
Number of Ki67+ nuclei (overlap)
Number of Ki67− nuclei (overlap)
```
### Per-nucleus results
``3DAnalysis_perNucleus_new.csv``

- Contains:
```
Image ID
Nucleus label ID
Mean Ki67 intensity
Integrated Ki67 intensity
Threshold value used
Ki67 classification (threshold-based)
Ki67 classification (overlap-based)
```


### Quality control images
Contain overlay stacks for visual inspection.
- Saved in:
``output/merge/``




