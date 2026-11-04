# ============================================================
# Dockerfile — Web API Quản Lý Nhà Hàng & POS
# Tác giả: 0859 - Nguyễn Quốc Huy
# Tương thích: Render.com / Railway.app / MonsterASP / Docker local
# ============================================================

# Stage 1: Build & Publish
FROM mcr.microsoft.com/dotnet/sdk:10.0 AS build
WORKDIR /src

# Copy project files và restore dependencies
COPY ["0859_NguyenQuocHuy_WebAPI/DA Restaurant Api.csproj", "0859_NguyenQuocHuy_WebAPI/"]
COPY ["0859_NguyenQuocHuy_WebAPI/0859_NguyenQuocHuy_WebAPI.csproj", "0859_NguyenQuocHuy_WebAPI/"]
RUN dotnet restore "0859_NguyenQuocHuy_WebAPI/DA Restaurant Api.csproj"

# Copy toàn bộ mã nguồn và publish release
COPY . .
WORKDIR "/src/0859_NguyenQuocHuy_WebAPI"
RUN dotnet publish "DA Restaurant Api.csproj" -c Release -o /app/publish /p:UseAppHost=false

# Stage 2: Runtime image nhẹ
FROM mcr.microsoft.com/dotnet/aspnet:10.0 AS final
WORKDIR /app

# Render.com / Railway tự inject $PORT
ENV ASPNETCORE_ENVIRONMENT=Production
ENV UseSqlite=true

EXPOSE 8080
EXPOSE 5276

COPY --from=build /app/publish .
COPY --from=build /src/0859_NguyenQuocHuy_WebAPI/RestaurantPOS.db ./RestaurantPOS.db

ENTRYPOINT ["dotnet", "DA_Restaurant_Api.dll"]
