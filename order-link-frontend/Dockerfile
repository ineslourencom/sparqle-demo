# Use a Node.js image to build the frontend
FROM node:alpine AS build

# Set the working directory inside the build container
WORKDIR /app

# Copy package.json for dependency installation
COPY package.json ./

# Install dependencies
RUN npm install

# Copy the rest of the application source code
COPY . .

# Build the frontend application
RUN npm run build

# Use an Nginx image to serve the built application
FROM nginx:alpine

COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80