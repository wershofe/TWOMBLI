library(circular)

cmd <- commandArgs(trailingOnly = TRUE);

df <- read.csv(cmd);

df <- df[,c(1,2)]
colnames(df) <- c("Bin","ProbDensity")
df$Bin <- 2*df$Bin + 180 # To account for antiparallel alignment ie n and n+180 degrees are aligned
df$BinRadian <- df$Bin * (pi/180)

# Convert bins to pixels by using image size to go from prob density to density
h <- 2048
w <- 2048
df$Density <- round(df$ProbDensity * h * w)

# Convert dataframe into vector
vec <- numeric()
for(i in 1:nrow(df))
{
  tempVec <- rep(df$BinRadian[i], times = df$Density[i])
  vec <- c(vec, tempVec)
}

#Alignment
rBar <- angular.deviation(vec, na.rm = FALSE)

print(rBar[1])