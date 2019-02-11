#### Evaulating the Property distribution over time (tick) ####

#### Libraries ####
library(readr)
library(ggplot2)
library(dplyr)
library(ggthemes)

options(scipen = 999)

#### Load Data ####

snapshot_property <- read_csv("~/Desktop/AMONG_2019/snapshot_property.txt", col_types = cols(value_initial = col_integer()))
snapshot_housholds <- read_csv("~/Desktop/AMONG_2019/snapshot_housholds.txt")
snapshot_global <- read_csv("~/Desktop/AMONG_2019/snapshot_global.txt")

View(snapshot_global)

#### Eval Properties ####

properties_mean_Value = snapshot_property %>%
  group_by(tick) %>%                                                            # group by tick
  summarise(mean_Value = mean(value), mean_ValueInitial = mean(value_initial),mean_ValueProjected = mean(value_projected),mean_ValuePreviously = mean(value_previous), 
            variance_Value = var(value),variance_ValueInitial = var(value_initial),variance_ValueProjected = var(value_projected), variance_ValuePreviously = mean(value_previous)
            )

properties_mean_Value = properties_mean_Value %>%
  arrange(tick)

plot_means = ggplot(properties_mean_Value, aes(tick)) + 
  geom_line(aes(y = mean_Value, colour = "mean_Value")) + 
  geom_line(aes(y = mean_ValueInitial, colour = "mean_ValueInitial")) + 
  #geom_line(aes(y = mean_ValueProjected, colour = "mean_ValueProjected"))+
  geom_line(aes(y = mean_ValuePreviously, colour = "mean_ValuePreviously"))
plot_means

plot_variance = ggplot(properties_mean_Value, aes(tick)) + 
  geom_line(aes(y = variance_Value, colour = "variance_Value")) + 
  geom_line(aes(y = variance_ValueInitial, colour = "variance_ValueInitial")) + 
  #geom_line(aes(y = variance_ValueProjected, colour = "variance_ValueProjected"))+
  geom_line(aes(y = variance_ValuePreviously, colour = "variance_ValuePreviously"))
plot_variance

#### Eval Housholds ####

households_mean_Value = snapshot_housholds %>%
  group_by(tick) %>%                                                            # group by tick
  summarise(mean_assets = mean(asset), mean_ValueInitial = mean(asset_initial),mean_investmentHorizon = mean(investmenthorizon)
  )

households_mean_Value = households_mean_Value %>%
  arrange(tick)


plot_houshold_variance = ggplot(households_mean_Value, aes(tick)) + 
  geom_line(aes(y = mean_assets, colour = "mean_assets")) + 
  geom_line(aes(y = mean_ValueInitial, colour = "mean_ValueInitial")) + 
  geom_line(aes(y = mean_investmentHorizon, colour = "mean_investmentHorizon"))
plot_houshold_variance

hist(snapshot_housholds$investmenthorizon, 50)


bin = seq(0,10000000,by = 10000)
housholdLatest = subset(snapshot_housholds,snapshot_housholds$tick == max(snapshot_housholds$tick))
hist(housholdLatest$asset_initial, xlim = c(min(bin), max(bin)), breaks = length(bin))
hist(housholdLatest$asset, xlim = c(min(bin), max(bin)), breaks = length(bin))
asset = as.data.frame(housholdLatest$asset)
asset_init = as.data.frame(housholdLatest$asset_initial)

asset$class = 'now'
asset_init$class = 'init'
names = c('asset','class')
colnames(asset) = names
colnames(asset_init) = names
householdHist = rbind(asset,asset_init)

ggplot(householdHist, aes(asset, fill = class)) + 
  geom_density(alpha = 0.7, color=NA) +
  xlim(-1000000,10000000)



#### Eval Global ####


globalAssetsPlot = ggplot(snapshot_global, aes(tick)) +
  geom_line(aes(y = totalLiquidAssets, colour = "totalLiquidAssets")) + 
  geom_line(aes(y = totalPropertyAssets, colour = "totalPropertyAssets"))
globalAssetsPlot

snapshot_global$totalAssets = snapshot_global$totalLiquidAssets + snapshot_global$totalPropertyAssets
snapshot_global$totalInflationAssets = snapshot_global$totalAssets*0.98^(snapshot_global$tick/52)
globalTotalAssetsPlot = ggplot(snapshot_global, aes(tick)) +
  geom_line(aes(y = totalAssets, colour = "totalAssets")) +
  geom_line(aes(y = totalInflationAssets, colour = "totalInflationAssets"))
globalTotalAssetsPlot